package org.mjanowski

import akka.actor.typed.ActorSystem
import org.matsim.api.core.v01.network.Network
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

class MySim(val network: Network, val partitionsNumber: Int, val host: String, val port: Int) {

//  val addressConfig =
//    s"""akka.remote.artery.canonical.hostname=${host}
//          akka.remote.artery.canonical.port=${port}"""
//  val myConfig = ConfigFactory.parseString(addressConfig)
//  val regularConfig = ConfigFactory.load()
//  val combined = myConfig.withFallback(regularConfig)
//  val complete = ConfigFactory.load(combined);
//  val actorSystem = ActorSystem(SimMasterActor(), "system", complete)

//  val partitioner = new NetworkPartitioner(network)
//  val partitions : mutable.Map[Integer, Partition] = partitioner.partition(partitionsNumber).asScala

  def run(): Unit = {

  }

}
