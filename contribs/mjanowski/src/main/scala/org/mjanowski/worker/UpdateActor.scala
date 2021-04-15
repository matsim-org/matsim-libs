package org.mjanowski.worker

import akka.actor.DeadLetter
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, LogOptions}
import akka.event.Logging.LogLevel
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.network.{Link, Node}
import org.mjanowski.master.{MasterCommand, RegisterWorker, SimMasterActor}
import org.mjanowski.worker.SimWorkerActor.workerSim

import java.util
import scala.collection.JavaConverters.mapAsJavaMap
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava, SetHasAsJava}

object UpdateActor {

  var workerSim: WorkerSim = _

  def apply(workerSim: WorkerSim): Behavior[WorkerCommand] = {
    SimWorkerActor.workerSim = workerSim

    Behaviors.setup(context => {

      Behaviors.receiveMessage {

        case Update(workerId, moveVehicleDtos, replyTo) =>

          val accepted = workerSim.acceptVehicles(new Integer(workerId), moveVehicleDtos.asJava)
          replyTo ! Accepted(accepted)
          Behaviors.stopped

      }
    })
  }


}
