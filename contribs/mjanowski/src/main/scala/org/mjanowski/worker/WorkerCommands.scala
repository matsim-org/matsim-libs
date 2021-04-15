package org.mjanowski.worker

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import org.matsim.core.mobsim.qsim.qnetsimengine.{AcceptedVehiclesDto, EventDto, MoveVehicleDto}
import org.mjanowski.master.MySerializable

import java.util

sealed trait WorkerCommand extends MySerializable

case class ListingResponse(listing: Receptionist.Listing) extends WorkerCommand


//TOOD seq -> iterable???

case class AssignNodes(workerId: Int,
                       @JsonDeserialize(keyAs = classOf[Integer]) workersNodesIds: Map[Int, Seq[String]],
                       @JsonDeserialize(keyAs = classOf[Integer]) workersRefs: collection.Map[Int, ActorRef[WorkerCommand]]) extends WorkerCommand

case class StartIteration() extends WorkerCommand

case class SendUpdate(workerId: Int, seq: Seq[MoveVehicleDto], replyTo: ActorRef[WorkerCommand]) extends WorkerCommand

case class Update(workerId: Int, moveVehicleDtos: Seq[MoveVehicleDto], replyTo: ActorRef[WorkerCommand]) extends WorkerCommand

case class SendAccepted(workerId: Int, accepted: Map[String, util.Collection[util.List[AcceptedVehiclesDto]]]) extends WorkerCommand

case class Accepted(accepted: util.List[AcceptedVehiclesDto]) extends WorkerCommand

case class SendMovingNodesFinished() extends WorkerCommand

case class MovingNodesFinished() extends WorkerCommand

case class SendReadyForNextStep(finished: Boolean) extends WorkerCommand

case class ReadyForNextStep(finished: Boolean) extends WorkerCommand

case class SendEvents(events: Seq[EventDto]) extends WorkerCommand

case class SendFinishEventsProcessing() extends WorkerCommand

case class SendAfterMobsim() extends WorkerCommand
