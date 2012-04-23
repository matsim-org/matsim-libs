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
import org.matsim.core.basic.v01.IdImpl;
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
	private List<ParkAndRideFacility> pRfacilities = new ArrayList<ParkAndRideFacility>();

	private Integer maxCapacity = 40;
	
	private InternalInterface internalInterface;
	
	@Override
	public void doSimStep(double time) {
		System.out.println(this.prNr2vehicles);
		
		for (Integer prNr : this.prNr2ampel.keySet()){
			if (this.prNr2vehicles.get(prNr) > this.maxCapacity){
				this.prNr2ampel.get(prNr).setSignalStateAllTurningMoves(SignalGroupState.RED);
//				System.out.println("Ampel rot.");
			}
			
			else {
				this.prNr2ampel.get(prNr).setSignalStateAllTurningMoves(SignalGroupState.GREEN);
//				System.out.println("Ampel grün.");
			}
		}
	}

	@Override
	public void onPrepareSim() {
//   prNr ; A Rein	; A Raus  ; B Rein	   ; B Raus	
//		2 ; 2toPrA2 ; PrA2to2 ; PrA2toPrB2 ; PrB2toPrA2
//		4 ; 4toPrA4 ; PrA4to4 ; PrA4toPrB4 ; PrB4toPrA4
//		6 ; 6toPrA6 ; PrA6to6 ; PrA6toPrB6 ; PrB6toPrA6
		
//			Ampel			    prfacility
		
		pRfacilities.add(new ParkAndRideFacility(2, new IdImpl("2toPrA2"), new IdImpl("PrA2to2"), new IdImpl("PrA2toPrB2"), new IdImpl("PrB2toPrA2")));
		pRfacilities.add(new ParkAndRideFacility(4, new IdImpl("4toPrA4"), new IdImpl("PrA4to4"), new IdImpl("PrA4toPrB4"), new IdImpl("PrB4toPrA4")));
		pRfacilities.add(new ParkAndRideFacility(6, new IdImpl("6toPrA6"), new IdImpl("PrA6to6"), new IdImpl("PrA6toPrB6"), new IdImpl("PrB6toPrA6")));

		for (ParkAndRideFacility pr : this.pRfacilities){
			this.prNr2vehicles.put(pr.getNr(), 0);

			Id idArein = pr.getLinkArein();
			SignalizeableItem ampel = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(idArein) ;
			ampel.setSignalized(true);
			this.prNr2ampel.put(pr.getNr(), ampel);
		}
		System.out.println(this.prNr2vehicles);
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
		System.out.println("Reset.");
		this.pRfacilities.clear();
		this.prNr2ampel.clear();
		this.prNr2vehicles.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (ParkAndRideFacility pr : this.pRfacilities){
			Id id = pr.getLinkBrein();
			if (id.equals(event.getLinkId())){
				System.out.println("Car entered ParkAndRideFacilty (B rein): " + id.toString());
				int vehNr = this.prNr2vehicles.get(pr.getNr()) + 1;
				System.out.println("Auslastung: "+vehNr);
				this.prNr2vehicles.put(pr.getNr(), vehNr);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		for (ParkAndRideFacility pr : this.pRfacilities){
			Id id = pr.getLinkBrein();
			if (id.equals(event.getLinkId())){
				System.out.println("Car left ParkAndRideFacilty (B rein): " + id.toString());
				int vehNr = this.prNr2vehicles.get(pr.getNr()) - 1;
				System.out.println("Auslastung: "+vehNr);
				this.prNr2vehicles.put(pr.getNr(), vehNr);
			}
		}
	}

}
