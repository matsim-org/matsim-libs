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
package playground.ikaddoura.parkAndRide.pR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;



/**
 * @author Ihab
 *
 */
public class AdaptiveCapacityControl implements MobsimEngine, LinkEnterEventHandler, LinkLeaveEventHandler {

	private Map<Id, Integer> prId2vehicles = new HashMap<Id, Integer>();
	private Map<Id, SignalizeableItem> prId2ampel = new HashMap<Id, SignalizeableItem>();
	private List<ParkAndRideFacility> prFacilities = new ArrayList<ParkAndRideFacility>();

	private Integer maxCapacity = 20;
	
	private InternalInterface internalInterface;
	
	public AdaptiveCapacityControl(List<ParkAndRideFacility> prFacilities) {
		this.prFacilities = prFacilities;
	}

	@Override
	public void doSimStep(double time) {
		
		for (Id prId : this.prId2ampel.keySet()){
			if (this.prId2vehicles.get(prId) >= this.maxCapacity){
				this.prId2ampel.get(prId).setSignalStateAllTurningMoves(SignalGroupState.RED);
			} else {
				this.prId2ampel.get(prId).setSignalStateAllTurningMoves(SignalGroupState.GREEN);
			}
		}
	}

	@Override
	public void onPrepareSim() {

		for (ParkAndRideFacility pr : this.prFacilities){
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
		return this.internalInterface.getMobsim();
	}

	@Override
	public void reset(int iteration) {
		this.prId2ampel.clear();
		this.prId2vehicles.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (ParkAndRideFacility pr : this.prFacilities){
			Id id = pr.getPrLink3in();
			if (id.toString().equals(event.getLinkId().toString())){
				System.out.println("Car entered ParkAndRideFacilty: " + id.toString());
				int vehNr = this.prId2vehicles.get(pr.getId()) + 1;
				System.out.println("Auslastung: "+vehNr);
				this.prId2vehicles.put(pr.getId(), vehNr);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		for (ParkAndRideFacility pr : this.prFacilities){
			Id id = pr.getPrLink3out();

			if (id.toString().equals(event.getLinkId().toString())){
				System.out.println("Car left ParkAndRideFacilty: " + id.toString());
				int vehNr = this.prId2vehicles.get(pr.getId()) - 1;
				System.out.println("Auslastung: "+vehNr);
				this.prId2vehicles.put(pr.getId(), vehNr);
			}
		}
	}

}
