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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

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
	private Map<Id, ModeData> modesData;
	private ModeData globalData;
	
	public static Id studiedMeasuringPointLinkId = new IdImpl(0);
	
	
	private boolean permanentRegime;
	private double permanentDensity;
	private double permanentAverageVelocity;
	private double permanentFlow;
	
	public FundamentalDiagramsNmodes(Scenario sc, Map<Id, ModeData> modesData){
		this.scenario =  sc;
		this.modesData = modesData;
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).initDynamicVariables();
		}
		this.globalData = new ModeData();
		this.globalData.setnumberOfAgents(sc.getPopulation().getPersons().size());
		this.globalData.initDynamicVariables();
		this.permanentRegime = false;
	}

	@Override
	public void reset(int iteration) {	
		for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
			this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).reset();
		}
		this.globalData.reset();
		this.permanentRegime = false;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!(permanentRegime)){
			Id personId = event.getVehicleId();
			double pcu_person = 0.;
			
			//Disaggregated data updating methods
			Id transportMode = new IdImpl( (String) scenario.getPopulation().getPersons().get(personId).getCustomAttributes().get("transportMode"));
			this.modesData.get(transportMode).handle(event);
			pcu_person = this.modesData.get(transportMode).getVehicleType().getPcuEquivalents();
			
			//Aggregated data update
			double nowTime = event.getTime();
			if (event.getLinkId().equals(studiedMeasuringPointLinkId)){	
				//Updating global data
				this.globalData.updateFlow(nowTime, pcu_person);
				this.globalData.updateSpeedTable(nowTime, personId);
				//Waiting for all agents to be on the track before studying stability
				if ((this.globalData.getNumberOfDrivingAgents() == this.globalData.numberOfAgents) && (nowTime>23400)){	//TODO parametrize this correctly
					if (!(this.globalData.isSpeedStable())){
						this.globalData.checkSpeedStability(); 
					}
					if (!(this.globalData.isFlowStable())){
						this.globalData.checkFlowStability();
					}
			
					//Checking global stability
					boolean everythingStable = true;
					for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
						if (  !  ((this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).isSpeedStable())) && 
								 ((this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).isFlowStable())) ){
							everythingStable = false;
							System.out.println("Mode "+DreieckNmodes.NAMES[i]+" is not stable yet.");
						}
					}
					if (everythingStable){
						log.info("Global permanent regime attained");
						System.out.println("Global permanent regime attained");
						//TODO: write end variables or trust stability and do a simple write(getSpeed/Flow) in Dreieck?
						this.permanentDensity = this.globalData.numberOfAgents / DreieckNmodes.length * 3;
						this.permanentAverageVelocity = this.globalData.getActualAverageVelocity();
						this.permanentFlow = this.globalData.getActualFlow();
						//memorizing modal variables:
						for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
							this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).saveDynamicVariables();
						}
						this.permanentRegime = true;
						//How to: end simulation immediately? => solved by granting the mobsim agents access to permanentRegime
						//and making them exit the simulation as soon as permanentRegime is true.
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
	
	public double getPermanentDensity(){
		return this.permanentDensity;
	}
	
	public double getPermanentAverageVelocity(){
		return this.permanentAverageVelocity;
	}
	
	public double getPermanentFlow(){
		return this.permanentFlow;
	}

	public Map<Id, ModeData> getModesData() {
		return modesData;
	}
	
}