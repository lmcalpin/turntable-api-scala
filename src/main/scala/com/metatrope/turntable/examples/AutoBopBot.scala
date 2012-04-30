package com.metatrope.turntable.examples

import net.liftweb.json.JsonAST._
import com.metatrope.turntable.VoteDirection

// this is just an example bot to demonstrate how to use the API
// Turntable.FM does not allow autobop.
class AutoBopBot {
  val auth = "you need to supply this value"
  val userId = "you need to supply this value"

  val bot = new com.metatrope.turntable.Bot(auth, userId)
  def run = {
    // go to the industrial room
    bot.changeRoom("4df1058699968e6b8a00168d")

    // every time the song changes, vote it up
    bot.onNewSong { room =>
      println("Now playing " + room.currentSong.name + " by " + room.currentSong.artist + " in " + room.name)
      bot.vote(VoteDirection.Up)
    }

    // be a friendly bot
    bot.onUserRegistered {
      user => bot.speak("Hello, " + user.name)
    }
  }
}

object AutoBopBot {
  def main(args: Array[String]) {
    new AutoBopBot().run
  }
}
