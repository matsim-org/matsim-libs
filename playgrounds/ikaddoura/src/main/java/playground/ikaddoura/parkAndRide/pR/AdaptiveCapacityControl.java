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
package playground.ikaddoura.parkAndRide.strategyTest;

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

//	private Map<Id, Integer> linkId2vehicles = new HashMap<Id, Integer>();
	
	private InternalInterface internalInterface;
	private Id linkIdParkplatz = new IdImpl("3to4");
	private Integer maxCapacity = 0;
	private int vehNumber = 0;

	private SignalizeableItem ampel;
	
	@Override
	public void doSimStep(double time) {
		
		ampel.setSignalStateAllTurningMoves(SignalGroupState.RED);

		
//		if (vehNumber >= maxCapacity){
//			ampel.setSignalStateAllTurningMoves(SignalGroupState.RED);
////			System.out.println("Ampel rot.");
//		}
//		
//		else {
//			ampel.setSignalStateAllTurningMoves(SignalGroupState.GREEN);
////			System.out.println("Ampel grün.");
//		}
	}

	@Override
	public void onPrepareSim() {
		ampel = (SignalizeableItem) this.getMobsim().getNetsimNetwork().getNetsimLink(new IdImpl("3to4")) ;
		ampel.setSignalized(true);
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;		
	}
	@Override
	public Netsim getMobsim() {
		return this.internalInterface.getMobsim();
	}

	@Override
	public void reset(int iteration) {
		this.vehNumber = 0;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (linkIdParkplatz.equals(event.getLinkId())){
			System.out.println("vehNumber um eins erhöht.");
			this.vehNumber++;
		}
	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (linkIdParkplatz.equals(event.getLinkId())){
			System.out.println("vehNumber um eins verringert.");
			this.vehNumber--;
		}
	}

}
