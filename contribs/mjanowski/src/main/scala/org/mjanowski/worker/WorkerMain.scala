package org.mjanowski.worker

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.Node
import org.matsim.core.mobsim.qsim.qnetsimengine.{AcceptedVehiclesDto, EventDto, MoveVehicleDto}
import org.mjanowski.MySimConfig

import java.util
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
import scala.reflect.ClassTag

class WorkerMain(config: MySimConfig, workerSim: WorkerSim) {

  private val host = config.getMasterAddress
  private val port = config.getMasterPort
  private val addressConfig =
    s"""akka.remote.artery.canonical.hostname=0.0.0.0
          akka.remote.artery.canonical.port=0
          akka.cluster.seed-nodes = ["akka://system@${host}:${port}"]"""
  private val akkaConfig = ConfigFactory.parseString(addressConfig)
    .withFallback(ConfigFactory.load())
//  private val actorSystem = ActorSystem(Behaviors.logMessages(SimWorkerActor(workerSim)), "system", akkaConfig)
  private val actorSystem = ActorSystem(SimWorkerActor(workerSim), "system", akkaConfig)
  implicit val timeout: Timeout = 10.minutes
  implicit val scheduler = actorSystem.scheduler

  //  private val system2 = ActorSystem(DeadLetterActor(), "deadletter")
//  actorSystem.eventStream ! EventStream.Subscribe(system2)

  def startIteration(): Unit = {
    actorSystem ! StartIteration()
  }

  def update(workerId: Int, moveVehicleDtos: java.util.List[MoveVehicleDto], time: Double) = {
    val future: Future[Accepted] = actorSystem.ask(ref => SendUpdate(workerId, moveVehicleDtos.asScala.toSeq, ref))
      .mapTo[Accepted]
    Await.ready(future, Duration.Inf)
    future.value.get.get.accepted
  }

  def accepted(workerId: Int, accepted: java.util.Map[Id[Node], util.Collection[java.util.List[AcceptedVehiclesDto]]]): Unit = {
    val acceptedMap = accepted.asScala
      .map({ case (linkId, a) => (linkId.toString, a) })
      .toMap
    actorSystem ! SendAccepted(workerId, acceptedMap)
  }

  def sendFinished(): Unit = {
    actorSystem ! SendMovingNodesFinished()
  }

  def sendReadyForNextMoving(finished: Boolean): Unit = {
    actorSystem ! SendReadyForNextStep(finished)
  }

  def terminateSystem(): Unit = {
    actorSystem.terminate()
  }

  def sendEvents(events : java.util.List[EventDto]): Unit = {
    actorSystem ! SendEvents(events.asScala.toSeq)
  }

  def sendFinishEventsProcessing(): Unit = {
    actorSystem ! SendFinishEventsProcessing()
  }

  def sendAfterMobsim(): Unit = {
    actorSystem ! SendAfterMobsim()
  }

}
