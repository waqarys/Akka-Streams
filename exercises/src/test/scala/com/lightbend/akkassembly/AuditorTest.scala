package com.lightbend.akkassembly

import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.EventFilter
import org.scalatest.freespec.AnyFreeSpec

import scala.collection.immutable.Seq
import scala.concurrent.duration._

class AuditorTest extends AnyFreeSpec with AkkaSpec {

  "count" - {
    "should return zero if the stream is empty" in {
      val auditor = new Auditor

      val count = Source.empty[Int].runWith(auditor.count).futureValue

      assert(count === 0)
    }
    "should count elements in the stream" in {
      val auditor = new Auditor

      val count = Source(1 to 10).runWith(auditor.count).futureValue

      assert(count === 10)
    }
  }

  "log" - {
    "should log nothing if the source is empty." in {
      implicit val adapter = system.log
      val auditor = new Auditor

      EventFilter.debug(occurrences = 0).intercept {
        Source.empty[Int].runWith(auditor.log)
      }
    }
    "should log all elements to the logging adapter." in {
      implicit val adapter = system.log
      val auditor = new Auditor

      EventFilter.debug(occurrences = 10).intercept {
        Source(1 to 10).runWith(auditor.log)
      }
    }
    "should log the exact element" in {
      implicit val adapter = system.log
      val auditor = new Auditor

      EventFilter.debug(occurrences = 1, message = "Message").intercept {
        Source.single("Message").runWith(auditor.log)
      }
    }
  }

  "sample" - {
    "should do nothing if the source is empty" in {
      val auditor = new Auditor
      val sampleSize = 100.millis

      val source = Source.empty[Car]

      source.via(auditor.sample(sampleSize))
        .runWith(TestSink.probe[Car])
        .request(10)
          .expectComplete()
    }
    "should return all elements if they appear within the sample period." in {
      val auditor = new Auditor
      val sampleSize = 100.millis
      val expectedCars = 10

      val (source, sink) = TestSource.probe[Car]
        .via(auditor.sample(sampleSize))
        .toMat(TestSink.probe[Car])(Keep.both)
        .run()

      sink.request(20)

      (1 to expectedCars).foreach(_ => source.sendNext(
        Car(SerialNumber(), Color("000000"), Engine(), Seq.fill(4)(Wheel()), None)
      ))
      source.sendComplete()

      sink.expectNextN(expectedCars)
      sink.expectComplete()
    }
    "should ignore elements that appear outside the expected sample period." in {
      val auditor = new Auditor
      val sampleSize = 100.millis
      val expectedCars = 3

      val (source, sink) = TestSource.probe[Car]
        .via(auditor.sample(sampleSize))
        .toMat(TestSink.probe[Car])(Keep.both)
        .run()

      sink.request(20)

      (1 to expectedCars).foreach(_ => source.sendNext(
        Car(SerialNumber(), Color("000000"), Engine(), Seq.fill(4)(Wheel()), None)
      ))
      Thread.sleep(sampleSize.toMillis * 2)

      source.sendNext(
        Car(SerialNumber(), Color("000000"), Engine(), Seq.fill(4)(Wheel()), None)
      )
      source.sendComplete()

      sink.expectNextN(expectedCars)
      sink.expectComplete()
    }
  }

  "audit" - {
    "should return zero if there are no cars in the stream" in {
      val auditor = new Auditor()

      val cars = Source.empty[Car]
      val sampleSize = 100.millis

      val result = auditor.audit(cars, sampleSize).futureValue

      assert(result === 0)
    }
    "should return all cars if they are within the sample period" in {
      val auditor = new Auditor()

      val expectedQuantity = 10
      val cars = Source.repeat(
        Car(SerialNumber(), Color("000000"), Engine(), Seq.fill(4)(Wheel()), None)
      ).take(expectedQuantity)
      val sampleSize = 100.millis

      val result = auditor.audit(cars, sampleSize).futureValue

      assert(result === expectedQuantity)
    }
    "should limit the cars to only those that are within the sample period" in {
      val auditor = new Auditor()

      def generateCars(quantity: Int) = {
        (1 to quantity).map(_ =>
          Car(SerialNumber(), Color("000000"), Engine(), Seq.fill(4)(Wheel()), None)
        )
      }

      val expectedQuantity = 5
      val sampleSize = 450.millis

      val expectedCars = Source(generateCars(expectedQuantity))
      val unexpectedCars = Source(generateCars(expectedQuantity))
        .initialDelay(sampleSize * 2)

      val cars = expectedCars.concat(unexpectedCars);

      val result = auditor.audit(cars, sampleSize).futureValue

      assert(result === 5)
    }
  }
}
