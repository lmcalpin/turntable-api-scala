/**
 * Copyright 2011 Lawrence McAlpin
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the PostgreSQL Global Development Group nor the names
 *    of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.metatrope.turntable

import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.Date

import akka.actor.Actor._
import akka.actor.Actor
import akka.routing.Routing._
import scala.collection.mutable.Buffer

import com.metatrope.turntable.VoteDirection._
import com.metatrope.turntable.util._

import net.liftweb.json.JsonDSL._
import net.liftweb.json._

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

  val messages = scala.collection.mutable.Map[String, JsonPayload => Any]()
  val commandListeners = scala.collection.mutable.Map[String, JsonPayload => Unit]()
  var readyListener: Option[() => Unit] = None

  var currentRoom: Option[String] = None
  val clientid = new Date().getTime + "-0.59633534294921572"

  // main message processing actor
  val messageProcessor = actorOf(new MessageProcessor).start()

  // start connection
  var wsc: WebSocketClient = connect
 
  /**
   * Go to a new room.
   */
  def changeRoom(roomId: String) = {
    debug("Changed room to " + roomId)
    currentRoom = Some(roomId)
    wsc.close
    wsc = connect
    waitForResponse("room.register") { r => }
  }

  /**
   * Returns information about available rooms.
   */
  def listRooms(): List[Room] = {
    waitForResponse("room.list_rooms") { r =>
      val jRooms = (r.json \ "rooms").children
      var rooms = Buffer[Room]()
      jRooms.foreach(roomJson => {
        val r = new Room(roomJson)
        rooms += r
      })
      rooms.toList
    }
  }

  /**
   * Returns information about the current room.
   */
  def roomInfo(): Room = {
    waitForResponse("room.info") { r => new Room(r.json \ "room") }
  }

  /**
   * Views your profile.
   */
  def userInfo(): User = {
    waitForResponse("user.info") { r => new User(r.json) }
  }

  /**
   * Views a specified user's profile.
   */
  def userInfo(userid: String) = {
    waitForResponse("user.info", ("userid" -> userid)) { r => new User(r.json) }
  }

  /**
   * Makes your bot chat.
   */
  def speak(text: String) = {
    req("room.speak", ("text" -> text))
  }

  /**
   * Changes your name.
   */
  def modifyName(name: String) = {
    req("user.modify", ("name" -> name))
  }

  /**
   * Do your democratic duty.  'dir' should be 'up' or 'down'.
   */
  def vote(dir: VoteDirection) = {
    val ri = roomInfo()
    val vh = hash(currentRoom.get + dir + ri.currentSong.id)
    val th = hash(scala.math.random.toString)
    val ph = hash(scala.math.random.toString)
    req("room.vote", ("val" -> dir.toString) ~ ("vh" -> vh) ~ ("th" -> th) ~ ("ph" -> ph))
  }

  private def roomNow() = { req("room.now") }

  /**
   * Invokes f whenever someone speaks in the chat window.
   */
  def listen(f: Chat => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      val x = new Chat(r.json)
      f(x)
    }
    commandListeners.put("speak", callbackWrapper)
  }

  /**
   * Invokes f whenever someone votes a song up or down.
   */
  def onUpdateVotes(f: VoteCount => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      val x = new VoteCount(r.json)
      f(x)
    }
    commandListeners.put("update_votes", callbackWrapper)
  }

  /**
   * Invokes f whenever someone votes a song up or down.
   */
  def onNewSong(f: Room => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      val x = new Room(r.json \ "room")
      f(x)
    }
    commandListeners.put("newsong", callbackWrapper)
  }

  /**
   * Invokes f whenever a Dj steps up.
   */
  def onAddDj(f: User => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      val x = new User(r.json \ "user")
      f(x)
    }
    commandListeners.put("add_dj", callbackWrapper)
  }

  /**
   * Invokes f whenever a Dj steps down.
   */
  def onRemDj(f: User => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      val x = new User(r.json \ "user")
      f(x)
    }
    commandListeners.put("rem_dj", callbackWrapper)
  }

  /**
   * Invokes f whenever someone enters the room.
   */
  def onUserRegistered(f: User => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      val x = new User(r.json \ "user")
      f(x)
    }
    commandListeners.put("registered", callbackWrapper)
  }

  /**
   * Invokes f whenever someone leaves the room.
   */
  def onUserDeregistered(f: User => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      val x = new User(r.json \ "user")
      f(x)
    }
    commandListeners.put("deregistered", callbackWrapper)
  }

  /**
   * Invokes f whenever someone queues the currently playing
   * song (this results in a heart floating over their head in
   * the standard web UI.)
   */
  def onSnagged(f: String => Unit) = {
    val callbackWrapper: JsonPayload => Unit = { r =>
      r.json \ "userid"
    }
    commandListeners.put("snagged", callbackWrapper)
  }

  /**
   * Invoked after we are authenticated.
   */
  def onReady(f: () => Unit) = {
    readyListener = Some(f)
  }

  /**
   * Emulate a synchronous request to Turntable.fm.
   * Waits until we get a response.  Responses are associated with the
   * original sender by the 'msgid' field in the JSON payload.
   */
  private def waitForResponse[A](api: String, params: JObject = null)(f: JsonPayload => A): A = {
    val cd = new CountDownLatch(1)
    val fut = scala.actors.Futures.future[A] {
      var reply: Option[A] = None
      val callback: JsonPayload => A = { r =>
        val ret = f(r)
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

  /**
   * Send a request to Turntable.fm.  The respone will be processed asynchronously.
   */
  private def req[T](api: String, params: JObject = null, callback: Option[JsonPayload => T] = None) = {
    val messageId = nextMessageId
    var jsonMessage = ("api" -> api) ~ ("userid" -> userid) ~ ("clientid" -> clientid) ~ ("userauth" -> auth) ~ ("msgid" -> messageId)
    currentRoom map { r => jsonMessage = jsonMessage ~ ("roomid" -> r) }
    if (params != null)
      jsonMessage = jsonMessage ~ params
    val jsonString = compact(render(jsonMessage))
    val turntableMessage = "~m~" + jsonString.length + "~m~" + jsonString
    debug("Sending message #" + messageId + " => " + turntableMessage)
    callback map { c => messages(messageId) = c }
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
        try {
          val idx = m indexOf '{'
          debug("Received message: " + m)
          if (idx >= 0) {
            val jsonStr = m substring (idx)
            if (jsonStr != null) {
              val parsedJson = JsonParser.parse(jsonStr)
              val reply = new JsonPayload(parsedJson)
              messageProcessor ! reply
            }
          } else {
            if (m == "~m~10~m~no_session") {
              actorOf(new BotProcessor).start() ! Authenticate
            } else {
              // this is probably a heartbeat message
              // let the server know we're still here
              roomNow
            }
          }
        } catch {
          case t => error(t)
        }
      }
    })
    newClient.connect
    newClient
  }

  case class Authenticate
  case class Reply(message: JsonPayload)
  case class Command(message: JsonPayload)
  case class Ready

  class MessageProcessor extends Actor {
    def receive = {
      case reply: JsonPayload => {
        val command: String = reply.command
        val msgid: String = reply.msgid
        command match {
          case null =>
          case "killdashnine" => {
            info("Disconnecting because this user has been logged on elsewhere.")
            sys.exit(-1)
          }
          case _ => {
            // see if we registered a listener callback for this particular command
            commandListeners.get(command) map { f => actorOf(new BotProcessor).start() ! Command(reply) }
          }
        }
        // if there is a msgid, this might be a reply to a message we sent using
        // the method waitForResponse or by providing a custom callback to the req
        // method.
        messages.get(msgid) map { f => actorOf(new BotProcessor).start() ! Reply(reply) }
      }
    }
  }

  class BotProcessor extends Actor {
    def receive = {
      case Authenticate => {
        waitForResponse("user.authenticate") { r =>
          actorOf(new BotProcessor).start() ! Ready
        }
      }
      case Ready => {
        readyListener.map { f => f() }
      }
      case c: Command => {
        val message = c.message
        val command: String = message.command
        commandListeners.get(command) map { f => f(message) }
      }
      case m: Reply => {
        val message = m.message
        val msgid: String = message.msgid
        messages.get(msgid) map { f => messages.remove(msgid); f(message) }
      }
    }
  }
}
