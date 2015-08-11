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

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.vehicles.VehicleType;


/**
 * @author ssix
 * A class supposed to go attached to the GenerateFundametalDiagramData class.
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average 
 * velocity for each velocity group.
 */

class GlobalFlowDynamicsUpdator implements LinkEnterEventHandler {

//	private Scenario scenario;
	private Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> travelModesFlowData;
	private TravelModesFlowDynamicsUpdator globalFlowData;

	public final static Id<Link> flowDynamicsMeasurementLinkId = Id.createLinkId(0);

	private boolean permanentRegime;


	/**
	 * @param sc
	 * @param travelModeFlowDataContainer
	 * container to store static properties of vehicles and dynamic flow properties during simulation 
	 */
	public GlobalFlowDynamicsUpdator(/*Scenario sc,*/ Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> travelModeFlowDataContainer){
//		this.scenario =  sc;
		this.travelModesFlowData = travelModeFlowDataContainer;
		for (int i=0; i<GenerateFundamentalDiagramData.TRAVELMODES.length; i++){
			this.travelModesFlowData.get(Id.create(GenerateFundamentalDiagramData.TRAVELMODES[i],VehicleType.class)).initDynamicVariables();
		}
		this.globalFlowData = new TravelModesFlowDynamicsUpdator();
		this.globalFlowData.setnumberOfAgents(GenerateFundamentalDiagramData.person2Mode.size());
		this.globalFlowData.initDynamicVariables();
		this.permanentRegime = false;
	}

	@Override
	public void reset(int iteration) {	
		for (int i=0; i<GenerateFundamentalDiagramData.TRAVELMODES.length; i++){
			this.travelModesFlowData.get(Id.create(GenerateFundamentalDiagramData.TRAVELMODES[i],VehicleType.class)).reset();
		}
		this.globalFlowData.reset();
		this.permanentRegime = false;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!(permanentRegime)){
			Id<Person> personId = Id.createPersonId(event.getVehicleId());
			double pcu_person = 0.;

			String travelMode = GenerateFundamentalDiagramData.person2Mode.get(personId);
			
			Id<VehicleType> transportMode = Id.create(travelMode,VehicleType.class);
			this.travelModesFlowData.get(transportMode).handle(event);
			pcu_person = this.travelModesFlowData.get(transportMode).getVehicleType().getPcuEquivalents();

			//Aggregated data update
			double nowTime = event.getTime();
			if (event.getLinkId().equals(flowDynamicsMeasurementLinkId)){				
				this.globalFlowData.updateFlow900(nowTime, pcu_person);
				this.globalFlowData.updateSpeedTable(nowTime, personId);
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
					for (int i=0; i<GenerateFundamentalDiagramData.TRAVELMODES.length; i++){
						Id<VehicleType> localVehicleType = Id.create(GenerateFundamentalDiagramData.TRAVELMODES[i],VehicleType.class);
						if (this.travelModesFlowData.get(localVehicleType).numberOfAgents != 0){
							if (! this.travelModesFlowData.get(localVehicleType).isSpeedStable() || !(this.travelModesFlowData.get(localVehicleType).isFlowStable())) {
								modesStable = false;
								break;
							} 
						}
					}
					if (modesStable){
						//Checking global stability
						if ( /*this.globalData.isSpeedStable() &&*/ this.globalFlowData.isFlowStable() ){
							//log.info("Global permanent regime attained");
							GenerateFundamentalDiagramData.log.info("========== Global permanent regime is attained");
							for (int i=0; i<GenerateFundamentalDiagramData.TRAVELMODES.length; i++){
								this.travelModesFlowData.get(Id.create(GenerateFundamentalDiagramData.TRAVELMODES[i],VehicleType.class)).saveDynamicVariables();
							}
							this.globalFlowData.setPermanentAverageVelocity(this.globalFlowData.getActualAverageVelocity());
							//this.permanentFlow = this.getActualFlow();
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

	public boolean isPermanent(){
		return permanentRegime;
	}

	public TravelModesFlowDynamicsUpdator getGlobalData(){
		return this.globalFlowData;
	}

	public Map<Id<VehicleType>, TravelModesFlowDynamicsUpdator> getModesData() {
		return travelModesFlowData;
	}

}