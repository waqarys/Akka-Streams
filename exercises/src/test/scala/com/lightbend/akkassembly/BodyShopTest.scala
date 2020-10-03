package com.lightbend.akkassembly

import akka.stream.scaladsl.Sink
import org.scalatest.freespec.AnyFreeSpec


import scala.concurrent.duration._

class BodyShopTest extends AnyFreeSpec with AkkaSpec {
  "cars" - {
    "should return cars at the expected rate" in {
      val bodyShop = new BodyShop(buildTime = 200.millis)

      val cars = bodyShop.cars
        .takeWithin(1100.millis)
        .runWith(Sink.seq)
        .futureValue

      assert(cars.size == 6)
    }
  }
}
