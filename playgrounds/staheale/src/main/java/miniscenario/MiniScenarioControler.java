/* *********************************************************************** *
 * project: org.matsim.*
 * MiniScenarioControler.java
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

package miniscenario;

import java.util.TreeMap;

import occupancy.FacilitiesOccupancyCalculator;
import occupancy.FacilityOccupancy;
import miniscenario.AgentInteraction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import scoring.AgentInteractionScoringFunctionFactory;

public class MiniScenarioControler extends Controler {
	private TreeMap<Id, FacilityOccupancy> facilityOccupancies;
	

	public MiniScenarioControler(final String[] args) {
		super(args);
		super.setOverwriteFiles(true) ;
	}

	public static void main(String[] args) {
		MiniScenarioControler controler = new MiniScenarioControler( args ) ;	
		controler.run();

	}
	
	protected void setUp() {

		this.setCreateGraphs(false);
		this.setDumpDataAtEnd(false);
		this.setWriteEventsInterval(0);
		
	    ObjectAttributes attributes = new ObjectAttributes();
	    ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(attributes);
		attributesReader.parse("./input/miniScenarioFacilityAttributes.xml");
		
		// get objects that are required as parameter for the AgentInteractionScoringFunctionFactory 
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = this.getConfig().planCalcScore();
		ActivityFacilities facilities = this.getFacilities();
		Network network = this.getNetwork();

		// create the AgentInteractionScoringFunctionFactory
		AgentInteractionScoringFunctionFactory factory = new AgentInteractionScoringFunctionFactory(planCalcScoreConfigGroup, facilities, network, Double.parseDouble(this.getConfig().locationchoice().getScaleFactor()));

		// set the AgentInteractionScoringFunctionFactory as default in the controler 
		this.setScoringFunctionFactory(factory);
	    super.setUp();	
		this.facilityOccupancies = new TreeMap<Id, FacilityOccupancy>(); 
			
		addControlerListener(new FacilitiesOccupancyCalculator(this.facilityOccupancies, AgentInteraction.numberOfTimeBins, AgentInteraction.scaleNumberOfPersons));		    
	}
}
