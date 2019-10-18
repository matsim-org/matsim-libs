/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleAdaptiveControl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.parkAndRide;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;

import java.util.HashMap;
import java.util.Map;

/**
 * An adaptive traffic light observing the number of vehicles per park-and-ride facility.
 * 
 * @author ikaddoura
 *
 */
public class PRAdaptiveCapacityControl implements MobsimEngine, LinkEnterEventHandler, LinkLeaveEventHandler {
	
//	private static final Logger log = Logger.getLogger(PRAdaptiveCapacityControl.class);
	private Map<Id, Integer> prId2vehicles = new HashMap<Id, Integer>();
	private Map<Id, SignalizeableItem> prId2ampel = new HashMap<Id, SignalizeableItem>();
	private Map<Id<PRFacility>, PRFacility> id2prFacility = new HashMap<>();
	
	private InternalInterface internalInterface;
	
	public PRAdaptiveCapacityControl(Map<Id<PRFacility>, PRFacility> id2prFacility) {
		this.id2prFacility = id2prFacility;
	}

	@Override
	public void doSimStep(double time) {
		
		for (Id<PRFacility> prId : this.prId2ampel.keySet()){
			if (this.prId2vehicles.get(prId) >= this.id2prFacility.get(prId).getCapacity()){
				this.prId2ampel.get(prId).setSignalStateAllTurningMoves(SignalGroupState.RED);
			} else {
				this.prId2ampel.get(prId).setSignalStateAllTurningMoves(SignalGroupState.GREEN);
			}
		}
	}

	@Override
	public void onPrepareSim() {

		for (PRFacility pr : this.id2prFacility.values()){
			this.prId2vehicles.put(pr.getId(), 0);

			Id prLink2in = pr.getPrLink2in();
			SignalizeableItem ampel = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(prLink2in) ;
			ampel.setSignalized(true);
			this.prId2ampel.put(pr.getId(), ampel);
		}
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;		
	}

	public Netsim getMobsim() {
		return ((QSim) this.internalInterface.getMobsim());
	}

	@Override
	public void reset(int iteration) {
		this.prId2ampel.clear();
		this.prId2vehicles.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (PRFacility pr : this.id2prFacility.values()){
			Id id = pr.getPrLink3in();
			if (id.toString().equals(event.getLinkId().toString())){
//				log.info("Car entered ParkAndRideFacilty: " + id.toString());
				int vehNr = this.prId2vehicles.get(pr.getId()) + 1;
				this.prId2vehicles.put(pr.getId(), vehNr);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		for (PRFacility pr : this.id2prFacility.values()){
			Id id = pr.getPrLink3out();

			if (id.toString().equals(event.getLinkId().toString())){
//				log.info("Car left ParkAndRideFacilty: " + id.toString());
				int vehNr = this.prId2vehicles.get(pr.getId()) - 1;
				this.prId2vehicles.put(pr.getId(), vehNr);
			}
		}
	}

}
