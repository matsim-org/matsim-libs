package org.mjanowski.master

import akka.actor.typed.ActorRef
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto
import org.mjanowski.worker.WorkerCommand

trait MySerializable
sealed trait MasterCommand extends MySerializable

case class RegisterWorker(replyTo: ActorRef[WorkerCommand]) extends MasterCommand

case class Events(events: Seq[EventDto], sender: ActorRef[WorkerCommand]) extends MasterCommand

case class FinishEventsProcessing() extends MasterCommand

case class AfterMobsim() extends MasterCommand