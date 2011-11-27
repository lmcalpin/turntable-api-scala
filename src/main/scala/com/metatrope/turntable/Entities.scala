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

import net.liftweb.json.JsonAST._

trait JsonReader {
  implicit def jv2str(jv: JValue): String = if (jv == JNothing) null else jv.values.toString
}

class Reply(val json: JValue) extends JsonReader {
  val msgid:String = json \ "msgid"
  val command:String = json \ "command"
}

class Room(json: JValue) extends JsonReader {
  val now: String = json \ "now"
  val name: String = json \ "name"
  val description: String = json \ "description"
  val shortcut: String = json \ "shortcut"
  val currentSong: Song = new Song(json \ "metadata" \ "current_song")
  lazy val users: List[User] = readUsers
  lazy val songlog: List[Song] = readSonglog
  private def readUsers: List[User] = {
    val jUsers = json \ "users"
    jUsers.children.collect { case x => new User(x) }
  }
  private def readSonglog: List[Song] = {
    val jSongs = json \ "metadata" \ "songlog"
    jSongs.children.collect { case x => new Song(x) }
  }
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
  val djid: String = json \ "djid"
  val album: String = json \ "metadata" \ "album"
  val artist: String = json \ "metadata" \ "artist"
  val coverart: String = json \ "metadata" \ "coverart"
  val name: String = json \ "metadata" \ "song"
}

class VoteCount(json: JValue) extends JsonReader {
  val upvotes: String = json \ "room" \ "metadata" \ "upvotes"
  val downvotes: String = json \ "room" \ "metadata" \ "downvotes"
  val listeners: String = json \ "room" \ "metadata" \ "listeners"
  val votelog: List[Tuple2[String, String]] = {
    val votelograw = json \ "room" \ "metadata" \ "votelog"
    votelograw.children.collect { case x => { (x.apply(0):String,x.apply(1):String) } }
  }
}

object VoteDirection extends Enumeration {
  type VoteDirection = Value
  val Up = Value("up")
  val Down = Value("down")
}