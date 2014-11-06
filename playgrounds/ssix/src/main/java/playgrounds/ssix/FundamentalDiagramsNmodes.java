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

package playgrounds.ssix;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author ssix
 * A class supposed to go attached to the DreieckNmodes class.
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average 
 * velocity for each velocity group.
 */

public class FundamentalDiagramsNmodes implements LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(FundamentalDiagramsNmodes.class);
	
	private Scenario scenario;
	private Map<String, ModeData> modesData;
	private ModeData globalData;
	
	public final static Id<Link> studiedMeasuringPointLinkId = Id.create(0, Link.class);
		
	private boolean permanentRegime;

	
	public FundamentalDiagramsNmodes(Scenario sc, Map<String, ModeData> modesData){
		this.scenario =  sc;
		this.modesData = modesData;
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			this.modesData.get(DreieckNmodes.NAMES[i]).initDynamicVariables();
		}
		this.globalData = new ModeData();
		this.globalData.setnumberOfAgents(sc.getPopulation().getPersons().size());
		this.globalData.initDynamicVariables();
		this.permanentRegime = false;
	}

	@Override
	public void reset(int iteration) {	
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			this.modesData.get(DreieckNmodes.NAMES[i]).reset();
		}
		this.globalData.reset();
		this.permanentRegime = false;
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!(permanentRegime)){
			Id<Vehicle> personId = event.getVehicleId();
			double pcu_person = 0.;
			
			//Disaggregated data updating methods
			String transportMode = (String) scenario.getPopulation().getPersons().get(personId).getCustomAttributes().get("transportMode");
			this.modesData.get(transportMode).handle(event);
			pcu_person = this.modesData.get(transportMode).getVehicleType().getPcuEquivalents();
			
			//Aggregated data update
			double nowTime = event.getTime();
			if (event.getLinkId().equals(studiedMeasuringPointLinkId)){				
				//Updating global data
				//this.globalData.updateFlow(nowTime, pcu_person);
				this.globalData.updateFlow900(nowTime, pcu_person);
				this.globalData.updateSpeedTable(nowTime, personId);
				//Waiting for all agents to be on the track before studying stability
				if ((this.globalData.getNumberOfDrivingAgents() == this.globalData.numberOfAgents) && (nowTime>1800)){	//TODO parametrize this correctly
					/*//Taking speed check out, as it is not reliable on the global speed table
					 *  Maybe making a list of moving averages could be smart, 
					 *  but there is no reliable converging process even in that case. (ssix, 25.10.13)
					 * if (!(this.globalData.isSpeedStable())){
						this.globalData.checkSpeedStability(); 
						System.out.println("Checking speed stability in global data for: "+this.globalData.getSpeedTable());
					}*/
					if (!(this.globalData.isFlowStable())){
						this.globalData.checkFlowStability900();
					}
			
					//Checking modes stability
					boolean modesStable = true;
					for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
						if (this.modesData.get(DreieckNmodes.NAMES[i]).numberOfAgents != 0){
							if (this.modesData.get(DreieckNmodes.NAMES[i]).isSpeedStable()) {
								//System.out.println("Mode "+DreieckNmodes.NAMES[i]+" is speed stable, checking for flow...");
								if ( ! (this.modesData.get(DreieckNmodes.NAMES[i]).isFlowStable()) ){
									//System.out.println("Mode "+DreieckNmodes.NAMES[i]+" is not flow stable yet.");
									modesStable = false;
									break;
								} else {
									//System.out.println("Mode "+DreieckNmodes.NAMES[i]+" is also flow Stable!");
									continue;
								}
							} else {
								//System.out.println("Mode "+DreieckNmodes.NAMES[i]+" is still not speed stable.");
								modesStable = false;
								break;
							}
						} else {
							//System.out.println("Mode "+DreieckNmodes.NAMES[i]+" is empty thus declared permanent.");
							continue;
						}
					}
					if (modesStable){
						//Checking global stability
						if ( /*this.globalData.isSpeedStable() &&*/ this.globalData.isFlowStable() ){
							//log.info("Global permanent regime attained");
							System.out.println("Global permanent regime attained");
							for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
								this.modesData.get(DreieckNmodes.NAMES[i]).saveDynamicVariables();
							}
							this.globalData.setPermanentAverageVelocity(this.globalData.getActualAverageVelocity());
							//this.permanentFlow = this.getActualFlow();
							this.globalData.setPermanentFlow(this.globalData.getActualFlow900());
							double globalDensity = 0.;
							for (ModeData mode : this.modesData.values()){
								globalDensity += mode.getPermanentDensity();
							}
							this.globalData.setPermanentDensity(globalDensity);
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
	
	public ModeData getGlobalData(){
		return this.globalData;
	}

	public Map<String, ModeData> getModesData() {
		return modesData;
	}
	
}