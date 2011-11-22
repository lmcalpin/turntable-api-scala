package com.metatrope.turntable

import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonAST.JString
import net.liftweb.json.JsonAST.JObject

trait JsonReader {
  implicit def jv2str(jv: JValue): String = jv.values.toString
}

class Reply(val json: JValue) extends JsonReader {
  val msgid: String = json \ "msgid"
}

class Room(json: JValue) extends JsonReader {
  val now: String = json \ "now"
  val name: String = json \ "name"
  val description: String = json \ "description"
  val shortcut: String = json \ "shortcut"
  val currentSong: Song = new Song(json \\ "current_song")
}

class User(json: JValue) extends JsonReader {
  val name: String = json \ "name"
  val laptop: String = json \ "laptop"
  val userid: String = json \ "userid"
  val fans: String = json \ "fans"
  val avatarid: String = json \ "avatarid"
  val email: String = json \ "email"
  val points: String = json \ "points"
  val twitter: String = json \ "twitter"
  val facebook: String = json \ "facebook"
  val topartists: String = json \ "topartists"
  val website: String = json \ "website"
  val hangout: String = json \ "hangout"
}

class Chat(json: JValue) extends JsonReader {
  val userid: String = json \ "userid"
  val name: String = json \ "name"
  val text: String = json \ "text"
}

class Song(json: JValue) extends JsonReader {
  val id: String = json \ "_id"
  val album: String = json \ "album"
  val artist: String = json \ "artist"
  val coverart: String = json \ "coverart"
  val name: String = json \ "song"
}

class VoteCount(json: JValue) extends JsonReader {
  val upvotes: String = json \ "room" \ "metadata" \ "upvotes"
  val downvotes: String = json \ "room" \ "metadata" \ "downvotes"
}