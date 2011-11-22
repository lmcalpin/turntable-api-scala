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