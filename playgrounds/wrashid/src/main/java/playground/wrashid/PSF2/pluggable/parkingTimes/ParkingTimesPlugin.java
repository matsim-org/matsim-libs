/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingTimesPlugin.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.wrashid.PSF2.pluggable.parkingTimes;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

/**
 * usage: - add this event handler to events - only after end of simulation.
 * 
 * note: call reset method also when reading only from events file (e.g. reset(0)).
 * 
 * @author wrashid
 * 
 */

public class ParkingTimesPlugin implements AgentArrivalEventHandler, AgentDepartureEventHandler {

	// agent Id, linked list of parkingInterval
	LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals;

	public ParkingTimesPlugin(Controler controler) {
		controler.addControlerListener(new AfterMobSimParkingPluginCleaner(this));
	}

	/**
	 * When using this constructor, then have to call method
	 * "closeLastAndFirstParkingInterval" after the simulation ends by yourself.
	 */
	public ParkingTimesPlugin() {

	}

	public void closeLastAndFirstParkingIntervals() {
		for (Id personId:parkingTimeIntervals.getKeySet()){
			ParkingIntervalInfo firstParkingInterval = parkingTimeIntervals.get(personId).getFirst();
			ParkingIntervalInfo lastParkingInterval = parkingTimeIntervals.get(personId).getLast();
			checkFirstLastLinkConsistency(firstParkingInterval.getLinkId(),lastParkingInterval.getLinkId());
			firstParkingInterval.setArrivalTime(lastParkingInterval.getArrivalTime());
			parkingTimeIntervals.get(personId).removeLast();
		}
	}

	
	private void checkFirstLastLinkConsistency(Id firstParkingIntervalLinkId, Id lastParkingIntervalLinkId) {
		if (!firstParkingIntervalLinkId.equals(lastParkingIntervalLinkId)) {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}

	public void reset(int iteration) {
		parkingTimeIntervals=new LinkedListValueHashMap<Id, ParkingIntervalInfo>();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		ParkingIntervalInfo parkingIntervalInfo = new ParkingIntervalInfo();
		parkingIntervalInfo.setArrivalTime(event.getTime());
		parkingIntervalInfo.setLinkId(event.getLinkId());

		parkingTimeIntervals.put(event.getPersonId(), parkingIntervalInfo);

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (leavingFirstParking(event.getPersonId())) {
			initializeParkingTimeIntervalsForPerson(event.getPersonId(), event.getLinkId());
		}

		updateDepartureTimeInfo(event);
	}

	private void updateDepartureTimeInfo(AgentDepartureEvent event) {
		ParkingIntervalInfo lastParkingInterval = parkingTimeIntervals.get(event.getPersonId()).getLast();
		checkLinkConsistency(lastParkingInterval, event.getLinkId());
		lastParkingInterval.setDepartureTime(event.getTime());
	}

	private void checkLinkConsistency(ParkingIntervalInfo lastParkingInterval, Id linkId) {
		if (!lastParkingInterval.getLinkId().equals(linkId)) {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}

	private void initializeParkingTimeIntervalsForPerson(Id personId, Id linkId) {
		ParkingIntervalInfo parkingIntervalInfo = new ParkingIntervalInfo();
		parkingIntervalInfo.setLinkId(linkId);
		parkingTimeIntervals.put(personId, parkingIntervalInfo);
	}

	private boolean leavingFirstParking(Id personId) {
		return parkingTimeIntervals.containsKey(personId);
	}

}
