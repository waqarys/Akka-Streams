package com.lightbend.akkassembly

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import scala.concurrent.Future

class Factory(bodyShop: BodyShop,
              paintShop: PaintShop,
              engineShop: EngineShop,
              wheelShop: WheelShop,
              qualityAssurance: QualityAssurance,
              upgradeShop: UpgradeShop)
             (implicit system: ActorSystem) {
  def orderCars(quantity: Int): Future[Seq[Car]] = {
    bodyShop.cars
      .via(paintShop.paint.named("paint-stage"))
      .async
      .via(engineShop.installEngine.named("install-engine-stage"))
      .async
      .via(wheelShop.installWheels.named("install-wheels-stage"))
      .async
      .via(upgradeShop.installUpgrades.named("install-upgrades-stage"))
      .via(qualityAssurance.inspect.named("inspect-stage"))
      .take(quantity)
      .runWith(Sink.seq)
  }
}
