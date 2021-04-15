package org.mjanowski.master

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, LogOptions}
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.matsim.api.core.v01.network.Network
import org.mjanowski.MySimConfig
import org.slf4j.event.Level

class MasterMain(config: MySimConfig, network: Network, masterSim: MasterSim) {

  val host = config.getMasterAddress
  val port = config.getMasterPort
  val workersNumber = config.getWorkersNumber
  val addressConfig =
    s"""akka.remote.artery.canonical.hostname=${host}
          akka.remote.artery.canonical.port=${port}
          akka.cluster.seed-nodes = ["akka://system@${host}:${port}"]"""
  val akkaConfig = ConfigFactory.parseString(addressConfig)
    .withFallback(ConfigFactory.load())
  val actorSystem = ActorSystem(
    SimMasterActor(workersNumber, network, masterSim),
    "system",
    akkaConfig)
//val actorSystem = ActorSystem(
//  Behaviors.logMessages(LogOptions().withLevel(Level.INFO), SimMasterActor(workersNumber, network, masterSim)),
//  "system",
//  akkaConfig)

}
