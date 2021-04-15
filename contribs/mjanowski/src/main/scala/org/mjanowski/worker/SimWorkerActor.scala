package org.mjanowski.worker

import akka.actor.DeadLetter
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, LogOptions}
import akka.event.Logging.LogLevel
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.{Link, Node}
import org.mjanowski.master.{AfterMobsim, Events, FinishEventsProcessing, MasterCommand, RegisterWorker, SimMasterActor}
import org.mjanowski.worker.SimWorkerActor.workerSim

import java.util
import scala.collection.JavaConverters.mapAsJavaMap
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava, SetHasAsJava}

object SimWorkerActor {

  var master: ActorRef[MasterCommand] = _
  var workerSim: WorkerSim = _
  var workerRefs: collection.Map[Int, ActorRef[WorkerCommand]] = _

  var workerId: Int = _
  var canStartIteration: Boolean = false
  var assigned: Boolean = false

  def apply(workerSim: WorkerSim): Behavior[WorkerCommand] = {
    SimWorkerActor.workerSim = workerSim

    Behaviors.setup(context => {
      val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse)
      context.system.receptionist ! Receptionist.Subscribe(SimMasterActor.masterKey, listingResponseAdapter)

      Behaviors.receiveMessage {

        case ListingResponse(SimMasterActor.masterKey.Listing(listings)) =>
          listings.headOption.foreach(r => {
            master = r
            r ! RegisterWorker(context.self)
          })
          Behaviors.same

        case AssignNodes(workerId, workersNodesIds, workersRefs) =>
          SimWorkerActor.workerRefs = workersRefs
          val javaWorkersNodesId: Map[Integer, util.Set[Id[Node]]] =
            workersNodesIds.map({ case (k, v) => (Integer.valueOf(k), v.map(id => Id.create(id, classOf[Node])).toSet.asJava) })
          println("hura")
          println(workerId, workersNodesIds, workersRefs)
          this.workerId = workerId
          workerSim.setWorkerId(workerId)
          workerSim.setPartitions(javaWorkersNodesId.asJava)

          println("assigned = true")
          println(Thread.currentThread().getName());
          assigned = true
          if (canStartIteration)
            workerSim.runIteration()
          Behaviors.same

        case StartIteration() =>
          println("canStartIteration = true")
          println(Thread.currentThread().getName());
          SimWorkerActor.canStartIteration = true
          if (assigned)
            workerSim.runIteration()
          Behaviors.same

        case SendUpdate(workerId, moveVehicleDtos, replyTo) =>

          SimWorkerActor.workerRefs(workerId) ! Update(workerId, moveVehicleDtos, replyTo)
          Behaviors.same

        case m : Update =>
          //czy tutaj musi być aktor? może future?
          context.spawn(UpdateActor(workerSim), "update" + System.nanoTime()) ! m
          Behaviors.same

        case SendMovingNodesFinished() =>
          workerRefs.view.filterKeys(k => k != workerId)
            .foreach({ case (k, v) => v ! MovingNodesFinished() })
          Behaviors.same

        case MovingNodesFinished() =>
          workerSim.movingNodesFinished()
          Behaviors.same

        case SendReadyForNextStep(finished) =>
          workerRefs.view.filterKeys(k => k != workerId)
            .foreach({ case (k, v) => v ! ReadyForNextStep(finished) })
          Behaviors.same

        case ReadyForNextStep(finished) =>
          workerSim.readyForNextStep(finished)
          Behaviors.same

        case SendEvents(events) =>
          master ! Events(events, context.self)
          Behaviors.same

        case SendFinishEventsProcessing() =>
          master ! FinishEventsProcessing()
          Behaviors.same

        case SendAfterMobsim() =>
          master ! AfterMobsim()
          Behaviors.same


      }
    })
  }


}
