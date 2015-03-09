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

package playground.staheale.miniscenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.analysis.DistanceStats;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.staheale.occupancy.FacilitiesOccupancyCalculator;
import playground.staheale.occupancy.FacilityOccupancy;
import playground.staheale.scoring.AgentInteractionScoringFunctionFactory;

import java.util.TreeMap;

public class MiniScenarioControler extends Controler {
	private TreeMap<Id, FacilityOccupancy> facilityOccupancies = new TreeMap<Id, FacilityOccupancy>();;


	public MiniScenarioControler(final String[] args) {
		super(args);
		super.setOverwriteFiles(true) ;
	}

	public static void main(String[] args) {
		MiniScenarioControler controler = new MiniScenarioControler( args ) ;	
		controler.run();
	}

	protected void setUp() {

        this.getConfig().controler().setCreateGraphs(true);
        this.setDumpDataAtEnd(false);
		this.getConfig().controler().setWriteEventsInterval(0);

		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(attributes);
		attributesReader.parse("./input/testMiniScenario/testMiniScenarioFacilityAttributes.xml");

		//---------------location choice module---------------------

		this.getConfig().setParam("locationchoice", "restraintFcnFactor", "0.0");

		ActivitiesHandler defineFlexibleActivities = new ActivitiesHandler((DestinationChoiceConfigGroup) this.config.getModule("locationchoice"));
		ScaleEpsilon scaleEpsilon = defineFlexibleActivities.createScaleEpsilon();

		ActTypeConverter actTypeConverter = defineFlexibleActivities.getConverter();

		//---------------location choice module end-------------------

		// get objects that are required as parameter for the AgentInteractionScoringFunctionFactory 
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = this.getConfig().planCalcScore();

        ActivityFacilities facilities = getScenario().getActivityFacilities();
        Network network = getScenario().getNetwork();

		// create the AgentInteractionScoringFunctionFactory
		AgentInteractionScoringFunctionFactory factory = new AgentInteractionScoringFunctionFactory(this, this.config, planCalcScoreConfigGroup,
				facilities, network, Double.parseDouble(this.getConfig().findParam("locationchoice", "scaleFactor")),
				facilityOccupancies, attributes, scaleEpsilon, actTypeConverter,
				defineFlexibleActivities.getFlexibleTypes());


		// set the AgentInteractionScoringFunctionFactory as default in the controler 
		this.setScoringFunctionFactory(factory);
		super.setUp();	

		addControlerListener(new FacilitiesOccupancyCalculator(this.facilityOccupancies, AgentInteraction.numberOfTimeBins, AgentInteraction.scaleNumberOfPersons));

		this.addControlerListener(new DistanceStats(this.config, "best", "s", actTypeConverter, "car"));
		this.addControlerListener(new DistanceStats(this.config, "best", "l", actTypeConverter, "car"));
	}
}
