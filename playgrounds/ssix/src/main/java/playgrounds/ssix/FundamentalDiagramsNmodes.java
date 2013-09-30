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
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author ssix
 * A class supposed to go attached to the DreieckNmodes class.
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average 
 * velocity for each velocity group.
 */

public class FundamentalDiagramsNmodes implements LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(FunDiagramsWithPassing3modes.class);
	
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
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		Id personId = event.getPersonId();
		double pcu_person = 0.;
		
		//Disaggregated data updating methods
		Id transportMode = new IdImpl( (String) scenario.getPopulation().getPersons().get(personId).getCustomAttributes().get("transportMode"));
		this.modesData.get(transportMode).handle(event);//TODO adjust this if necessary
		pcu_person = this.modesData.get(transportMode).getVehicleType().getPcuEquivalents();
		
		//Aggregated data update
		double nowTime = event.getTime();
		if (event.getLinkId().equals(studiedMeasuringPointLinkId)){	
			//Updating global data
			this.globalData.updateFlow(nowTime, pcu_person);
			this.globalData.updateSpeed(nowTime, personId);
			this.globalData.checkSpeedStability();
			
			boolean stableModes = true;
			for (int i=0; i<DreieckNmodes.NUMBER_OF_MODES; i++){
				if (!(this.modesData.get(new IdImpl(DreieckNmodes.NAMES[i])).isSpeedStable())){
					stableModes = false;
				}
			}
			
		}
		
	}
	
}
