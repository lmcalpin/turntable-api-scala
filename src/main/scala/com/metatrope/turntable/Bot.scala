package com.metatrope.turntable

import java.net.URI
import java.util.Date
import scala.actors.Future
import scala.collection.mutable.Buffer
import com.metatrope.turntable.util._
import net.liftweb.json.JsonDSL._
import net.liftweb.json._
import scala.actors.Actor
import java.util.concurrent.CountDownLatch

/**
 * The bot requires an authentication key (obtained when logging on to Turntable.fm
 * via Facebook Connect), a userid, and the room your bot is in.  You can obtain
 * these values using the bookmarklet Alain Gilbert developed:
 * http://alaingilbert.github.com/Turntable-API/bookmarklet.html
 *
 * This class is based on the work done by Alain Gilbert:
 * https://github.com/alaingilbert/
 */
class Bot(auth: String, userid: String) extends Logger with JsonReader {
  """ Beep beep bzzt whirrr. """
  val chatservers = Array("chat2.turntable.fm", "chat3.turntable.fm")
  var isConnected = false

  private var msgId = 0

  val messages = scala.collection.mutable.Map[String, Reply => Any]()
  val listeners = scala.collection.mutable.Map[String, Reply => Unit]()

  var currentRoom: Option[String] = None
  val clientid = new Date().getTime + "-0.59633534294921572"

  var wsc: WebSocketClient = connect
  req("user.authenticate")

  def changeRoom(roomId: String) = {
    debug("Changed room to " + roomId)
    currentRoom = Some(roomId)
    wsc.close
    wsc = connect
    waitForResponse("room.register") { r => }
  }

  def listRooms(): List[Room] = {
    waitForResponse("room.list_rooms") { r =>
      val roomJsonArray = (r.json \ "rooms").children
      var rooms = Buffer[Room]()
      roomJsonArray.foreach(roomJson => {
        val r = new Room(roomJson)
        rooms += r
      })
      rooms.toList
    }
  }

  def roomInfo(): Room = {
    waitForResponse("room.info") { r => new Room(r.json \ "room") }
  }

  def userInfo(): User = {
    waitForResponse("user.info") { r => new User(r.json) }
  }

  def userInfo(userid: String) = {
    waitForResponse("user.info", ("userid" -> userid)) { r => new User(r.json) }
  }

  def speak(text: String) = {
    req("room.speak", ("text" -> text))
  }

  def modifyName(name: String) = {
    req("user.modify", ("name" -> name))
  }

  def roomNow() = { req("room.now") }

  /**
   * Invokes f whenever someone speaks in the chat window.
   */
  def listen(f: Chat => Unit) = {
    val callbackWrapper: Reply => Unit = { r =>
      val x = new Chat(r.json)
      f(x)
    }
    listeners.put("speak", callbackWrapper)
  }

  def onUpdateVotes(f: VoteCount => Unit) = {
    val callbackWrapper: Reply => Unit = { r =>
      val x = new VoteCount(r.json)
      f(x)
    }
    listeners.put("update_votes", callbackWrapper)
  }

  /**
   * Emulate an synchronous request to Turntable.fm.
   * Waits until we get a response.  Responses are associated with the
   * original sender by the 'msgid' field in the JSON payload.
   */
  def waitForResponse[A](api: String, params: JObject = null)(f: Reply => A): A = {
    import scala.actors.Actor._
    val cd = new CountDownLatch(1)
    val fut = scala.actors.Futures.future[A] {
      var msgid: Option[String] = None
      var reply: Option[A] = None
      val callback: Reply => A = { r =>
        val ret = f(r)
        msgid = Some(r.msgid)
        reply = Some(ret)
        cd.countDown
        ret
      }
      req(api, callback = Some(callback))
      cd.await
      reply.get
    }
    fut.apply()
  }

  private def req[T](api: String, params: JObject = null, callback: Option[Reply => T] = None) = {
    val messageId = nextMessageId
    var jsonMessage = ("api" -> api) ~ ("userid" -> userid) ~ ("clientid" -> clientid) ~ ("userauth" -> auth) ~ ("msgid" -> messageId)
    currentRoom map { r => jsonMessage = jsonMessage ~ ("roomid" -> r) }
    if (params != null)
      jsonMessage = jsonMessage ~ params
    val jsonString = compact(render(jsonMessage))
    val turntableMessage = "~m~" + jsonString.length + "~m~" + jsonString
    debug("Sending message #" + messageId + " => " + turntableMessage)
    callback match {
      case Some(c) => messages(messageId) = c
      case _ =>
    }
    wsc.send(turntableMessage)
  }

  private def hash(text: String) = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.update(text.getBytes)
    md.digest map (b => "%02x".format(b)) mkString
  }

  private def chatserver(str: String): String = {
    var c = 0;
    hash(str).foreach { cc =>
      c += cc.intValue;
    }
    val idx = c % chatservers.size
    return chatservers(idx)
  }

  private def nextMessageId: String = {
    msgId += 1
    return String.valueOf(msgId)
  }

  private def connect: WebSocketClient = {
    val selectedChatserver = chatserver(currentRoom.getOrElse(scala.math.random.toString))
    val uri = "ws://" + selectedChatserver + ":80/socket.io/websocket"
    val newClient = new WebSocketClient(new URI(uri))({
      case WebSocket.OnOpen => debug("OnOpen")
      case WebSocket.OnMessage(m) => {
        val idx = m indexOf '{'
        debug("Received message: " + m)
        if (idx >= 0) {
          val message = m substring (idx)
          if (message != null) {
            val jsonPayload = JsonParser.parse(message)
            val reply = new Reply(jsonPayload)
            val command: String = jsonPayload \\ "command"
            command match {
              case "killdashnine" => {
                info("Disconnecting because this user has been logged on elsewhere.")
                sys.exit(-1)
              }
              case _ => {
                listeners.get(command) map { f => f(reply) }
              }
            }
            val msgid: String = reply.msgid
            messages.get(msgid) map { callback =>
              messages.remove(msgid)
              callback(reply)
            }
          }
        } else {
          // this is probably a heartbeat message
          // let the server know we're still here
          roomNow
        }
      }
    })
    newClient.connect
    newClient
  }
}
