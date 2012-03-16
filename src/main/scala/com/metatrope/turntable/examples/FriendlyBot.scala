package com.metatrope.turntable.examples

import net.liftweb.json.JsonAST._
import com.metatrope.turntable.VoteDirection

// this simply compliments you whenever you play something
class FriendlyBot {
  val auth = "you need to supply this value"
  val userId = "you need to supply this value"

  val bot = new com.metatrope.turntable.Bot(auth, userId)
  def run = {
    // go to the industrial room
    bot.changeRoom("4df1058699968e6b8a00168d") 

    // every time the song changes, vote it up
    bot.onNewSong { room =>
      val userInfo = bot.getProfile(room.currentSong.djid)
      if (userInfo.twitter == "yourtwitteraccount")
        bot.speak("YourNameHere, you always play the best songs!")
    }
  }
}

object FriendlyBot {
  def main(args: Array[String]) {
    new FriendlyBot().run
  }
}
