package com.metatrope.turntable.examples

import net.liftweb.json.JsonAST._
import com.metatrope.turntable.VoteDirection

// this bot keeps you company if there is only one DJ playing
class LonelyDjBot {
  val auth = "you need to supply this value"
  val userId = "you need to supply this value"

  val bot = new com.metatrope.turntable.Bot(auth, userId)
  
  var djing = false
  
  def run = {
    // go to the industrial room
    bot.changeRoom("4df1058699968e6b8a00168d") 

    val roomInfo = bot.roomInfo()
    var djCount = roomInfo.djs.length
    
    // compliment a particular dj
    bot.onAddDj { user =>
      djCount += 1
      shouldDj(djCount)
    }
    bot.onRemDj { user =>
      djCount -= 1
      shouldDj(djCount)
    }
  }
  def shouldDj(djCount: Int) = {
    println("Djs: " + djCount)
    // if our friend is djing, and he is lonely, join him!
    if (djCount == 1) {
      if (djing) {
        bot.stepDown
        djing = false
      } else {
        bot.dj
        djing = true
      }
    }
    // if someone else joins the party, stop djing
    if (djCount > 2 && djing) {
      bot.stepDown
      djing = false
    }
  }
}

object LonelyDjBot {
  def main(args: Array[String]) {
    new LonelyDjBot().run
  }
}
