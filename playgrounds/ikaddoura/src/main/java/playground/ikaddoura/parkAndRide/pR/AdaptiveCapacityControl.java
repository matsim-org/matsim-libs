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

	private Map<Integer, Integer> prNr2vehicles = new HashMap<Integer, Integer>();
	private Map<Integer, SignalizeableItem> prNr2ampel = new HashMap<Integer, SignalizeableItem>();
	private List<ParkAndRideFacility> prFacilities = new ArrayList<ParkAndRideFacility>();

	private Integer maxCapacity = 10;
	
	private InternalInterface internalInterface;
	
	public AdaptiveCapacityControl(List<ParkAndRideFacility> prFacilities) {
		this.prFacilities = prFacilities;
	}

	@Override
	public void doSimStep(double time) {
		
		for (Integer prNr : this.prNr2ampel.keySet()){
			if (this.prNr2vehicles.get(prNr) >= this.maxCapacity){
				this.prNr2ampel.get(prNr).setSignalStateAllTurningMoves(SignalGroupState.RED);
			} else {
				this.prNr2ampel.get(prNr).setSignalStateAllTurningMoves(SignalGroupState.GREEN);
			}
		}
	}

	@Override
	public void onPrepareSim() {

		for (ParkAndRideFacility pr : this.prFacilities){
			this.prNr2vehicles.put(pr.getNr(), 0);

			Id getPrLink1in = pr.getPrLink1in(); // TODO: ??? pr.getPrLink2in() ??? (testen!)
			SignalizeableItem ampel = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(getPrLink1in) ;
			ampel.setSignalized(true);
			this.prNr2ampel.put(pr.getNr(), ampel);
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
		this.prNr2ampel.clear();
		this.prNr2vehicles.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (ParkAndRideFacility pr : this.prFacilities){
			Id id = pr.getPrLink2in();
			if (id.toString().equals(event.getLinkId().toString())){
				System.out.println("Car entered ParkAndRideFacilty: " + id.toString());
				int vehNr = this.prNr2vehicles.get(pr.getNr()) + 1;
				System.out.println("Auslastung: "+vehNr);
				this.prNr2vehicles.put(pr.getNr(), vehNr);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		for (ParkAndRideFacility pr : this.prFacilities){
			Id id = pr.getPrLink2out();

			if (id.toString().equals(event.getLinkId().toString())){
				System.out.println("Car left ParkAndRideFacilty: " + id.toString());
				int vehNr = this.prNr2vehicles.get(pr.getNr()) - 1;
				System.out.println("Auslastung: "+vehNr);
				this.prNr2vehicles.put(pr.getNr(), vehNr);
			}
		}
	}

}
