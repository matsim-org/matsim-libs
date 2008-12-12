package playground.wrashid.PDES3;

import org.matsim.population.Person;

import playground.wrashid.DES.DeadlockPreventionMessage;
import playground.wrashid.DES.Message;
import playground.wrashid.DES.MessageFactory;
import playground.wrashid.DES.Road;
import playground.wrashid.DES.Scheduler;
import playground.wrashid.DES.SimUnit;
import playground.wrashid.DES.Vehicle;

public class PVehicle extends Vehicle {

	private int currentZoneId = -1;
	private boolean ownsCurrentZone = true;

	public PVehicle(PScheduler scheduler, Person ownerPerson) {
		super(scheduler, ownerPerson);
	}

	public void sendMessage(Message m, SimUnit targetUnit,
			double messageArrivalTime, int zoneId, boolean ownsZone) {
		m.setSendingUnit(this);
		m.setReceivingUnit(targetUnit);
		m.setMessageArrivalTime(messageArrivalTime);
		((PScheduler) scheduler).schedule(m, zoneId, ownsZone);
	}
	
	public void _scheduleEnterRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory
				.getEnterRoadMessage(scheduler, this), road,
				scheduleTime, currentZoneId, ownsCurrentZone);
	}

	public void scheduleEndRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getEndRoadMessage(scheduler, this), road,
				scheduleTime, currentZoneId, ownsCurrentZone);
	}

	public void scheduleLeaveRoadMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getLeaveRoadMessage(scheduler, this), road,
				scheduleTime, currentZoneId, ownsCurrentZone);
	}

	public void scheduleEndLegMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getEndLegMessage(scheduler, this), road,
				scheduleTime, currentZoneId, ownsCurrentZone);
	}

	public void scheduleStartingLegMessage(double scheduleTime, Road road) {
		sendMessage(MessageFactory.getStartingLegMessage(scheduler, this),
				road, scheduleTime, currentZoneId, ownsCurrentZone);
	}

	public DeadlockPreventionMessage scheduleDeadlockPreventionMessage(
			double scheduleTime, Road road) {
		DeadlockPreventionMessage dpMessage = MessageFactory
				.getDeadlockPreventionMessage(scheduler, this);
		sendMessage(dpMessage, road, scheduleTime, currentZoneId,
				ownsCurrentZone);
		return dpMessage;
	}

	public int getCurrentZoneId() {
		return currentZoneId;
	}

	public void setCurrentZoneId(int currentZoneId) {
		this.currentZoneId = currentZoneId;
	}

	public boolean isOwnsCurrentZone() {
		return ownsCurrentZone;
	}

	public void setOwnsCurrentZone(boolean ownsCurrentZone) {
		this.ownsCurrentZone = ownsCurrentZone;
	}

}
