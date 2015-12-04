/* *********************************************************************** *
 * project: org.matsim.*
 * FundamentalDiagramsNmodes											   *
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

package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.VehicleType;


/**
 * @author ssix, amit
 * A class supposed to go attached to the GenerateFundametalDiagramData class.
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average 
 * velocity for each velocity group.
 */

class GlobalFlowDynamicsUpdator implements LinkEnterEventHandler, PersonDepartureEventHandler {
	
	private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> travelModesFlowData;
	private TravelModesFlowDynamicsUpdator globalFlowData;
	private final Map<Id<Person>, String> person2Mode = new HashMap<Id<Person>, String>();
	
	public final static Id<Link> FLOW_DYNAMICS_UPDATE_LINK = Id.createLinkId(0);

	private boolean permanentRegime;

	/**
	 * @param travelModeFlowDataContainer
	 * container to store static properties of vehicles and dynamic flow properties during simulation 
	 */
	GlobalFlowDynamicsUpdator( Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> travelModeFlowDataContainer){
		int totalAgents = 0;
		this.travelModesFlowData = travelModeFlowDataContainer;
		for (Id<VehicleType> vehTyp : travelModeFlowDataContainer.keySet()){
			this.travelModesFlowData.get(vehTyp).initDynamicVariables();
			totalAgents += this.travelModesFlowData.get(vehTyp).getnumberOfAgents();
		}
		this.globalFlowData = new TravelModesFlowDynamicsUpdator(this.travelModesFlowData.size());
		this.globalFlowData.setnumberOfAgents(totalAgents);
		this.globalFlowData.initDynamicVariables();
		this.permanentRegime = false;
	}

	@Override
	public void reset(int iteration) {	
		for (Id<VehicleType> vehTyp : travelModesFlowData.keySet()){
			this.travelModesFlowData.get(vehTyp).reset();
		}
		this.globalFlowData.reset();
		this.permanentRegime = false;
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		person2Mode.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!(permanentRegime)){
			String travelMode = person2Mode.get(event.getDriverId());

			Id<VehicleType> transportMode = Id.create(travelMode, VehicleType.class);
			this.travelModesFlowData.get(transportMode).handle(event);
			double pcuPerson = this.travelModesFlowData.get(transportMode).getVehicleType().getPcuEquivalents();

			//Aggregated data update
			double nowTime = event.getTime();
			if (event.getLinkId().equals(FLOW_DYNAMICS_UPDATE_LINK)){				
				this.globalFlowData.updateFlow900(nowTime, pcuPerson);
				this.globalFlowData.updateSpeedTable(nowTime, Id.createPersonId(event.getDriverId()));
				//Waiting for all agents to be on the track before studying stability
				if ((this.globalFlowData.getNumberOfDrivingAgents() == this.globalFlowData.numberOfAgents) && (nowTime>1800)){	//TODO parametrize this correctly
					/*//Taking speed check out, as it is not reliable on the global speed table
					 *  Maybe making a list of moving averages could be smart, 
					 *  but there is no reliable converging process even in that case. (ssix, 25.10.13)
					 * if (!(this.globalData.isSpeedStable())){
						this.globalData.checkSpeedStability(); 
						System.out.println("Checking speed stability in global data for: "+this.globalData.getSpeedTable());
					}*/
					if (!(this.globalFlowData.isFlowStable())){
						this.globalFlowData.checkFlowStability900();
					}

					//Checking modes stability
					boolean modesStable = true;
					for (Id<VehicleType> vehTyp : travelModesFlowData.keySet()){
						if (this.travelModesFlowData.get(vehTyp).numberOfAgents != 0){
							if (! this.travelModesFlowData.get(vehTyp).isSpeedStable() || !(this.travelModesFlowData.get(vehTyp).isFlowStable())) {
								modesStable = false;
								break;
							} 
						}
					}
					if (modesStable){
						//Checking global stability
						if ( /*this.globalData.isSpeedStable() &&*/ this.globalFlowData.isFlowStable() ){
							GenerateFundamentalDiagramData.LOG.info("========== Global permanent regime is attained");
							for (Id<VehicleType> vehTyp : travelModesFlowData.keySet()){
								this.travelModesFlowData.get(vehTyp).saveDynamicVariables();
							}
							this.globalFlowData.setPermanentAverageVelocity(this.globalFlowData.getActualAverageVelocity());
							this.globalFlowData.setPermanentFlow(this.globalFlowData.getCurrentHourlyFlow()); 
							double globalDensity = 0.;
							for (TravelModesFlowDynamicsUpdator mode : this.travelModesFlowData.values()){
								globalDensity += mode.getPermanentDensity();
							}
							this.globalFlowData.setPermanentDensity(globalDensity);
							this.permanentRegime = true;
							//How to: end simulation immediately? => solved by granting the mobsim agents access to permanentRegime
							//and making them exit the simulation as soon as permanentRegime is true.
						}
					}
				}
			}
		}
	}

	boolean isPermanent(){
		return permanentRegime;
	}

	TravelModesFlowDynamicsUpdator getGlobalData(){
		return this.globalFlowData;
	}
}