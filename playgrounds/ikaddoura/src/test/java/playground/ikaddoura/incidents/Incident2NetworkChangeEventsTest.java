/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.incidents;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */

public class Incident2NetworkChangeEventsTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void test1() throws XMLStreamException, IOException, ParseException{
		
		String networkFile = testUtils.getPackageInputDirectory() + "network.xml";
		String inputDirectory = testUtils.getPackageInputDirectory() + "incidentsXML/";
		String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis/";
		
		String startDateTime = "2016-03-15";
		String endDateTime = "2016-03-15";

		String crs = TransformationFactory.DHDN_GK4;
		
		IncidentDataAnalysis analysis = new IncidentDataAnalysis(
				networkFile,
				crs,
				inputDirectory,
				outputDirectory,
				false,
				false,
				true,
				startDateTime,
				endDateTime,
				true,
				startDateTime,
				endDateTime
				);
		
		analysis.run();
		
		// test traffic items
		
		Assert.assertEquals("Wrong number of traffic items.", 279, analysis.getTrafficItems().size());
		
		Assert.assertEquals("Wrong incident code.", "C1", analysis.getTrafficItems().get("936802552584210227").getTMCAlert().getPhraseCode());
		Assert.assertEquals("Wrong incident start time.", DateTime.parseDateTimeToDateTimeSeconds("2016-03-15 07:15:33"), DateTime.parseDateTimeToDateTimeSeconds(analysis.getTrafficItems().get("936802552584210227").getStartDateTime()), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong incident end time.", DateTime.parseDateTimeToDateTimeSeconds("2016-03-15 14:37:16"), DateTime.parseDateTimeToDateTimeSeconds(analysis.getTrafficItems().get("936802552584210227").getEndDateTime()), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong incident longitude coordinate.", 13.3052, Double.parseDouble(analysis.getTrafficItems().get("936802552584210227").getOrigin().getLongitude()), MatsimTestUtils.EPSILON);
	
		// test network change events
		
		Config config = ConfigUtils.createConfig();
		config.network().setTimeVariantNetwork(true);
		config.network().setInputFile(networkFile);
		config.network().setChangeEventsInputFile(outputDirectory + "networkChangeEvents_2016-03-15.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new NetworkWriter(scenario.getNetwork()).write(testUtils.getOutputDirectory() + "output-network.xml");
				
//		LinkImpl link = (LinkImpl) scenario.getNetwork().getLinks().get(Id.createLinkId("36087"));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl) scenario.getNetwork().getLinks().get(Id.createLinkId("36087"));
		
		Assert.assertEquals("Wrong capacity.", 4700., link.getCapacity(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Wrong capacity (after first network change event).", 0., link.getFlowCapacityPerSec(10 * 3600.), MatsimTestUtils.EPSILON);		
		Assert.assertEquals("Wrong freespeed (after first network change event).", 0.22227, link.getFreespeed(10 * 3600.), MatsimTestUtils.EPSILON);		
	
		Assert.assertEquals("Wrong capacity (after second network change event).", 1.306, link.getFlowCapacityPerSec(16 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong freespeed (after second network change event).", 13.88888888888888, link.getFreespeed(16 * 3600.), MatsimTestUtils.EPSILON);
		
	}
	
	@Test
	public final void test2() {

		Config config = ConfigUtils.createConfig();
		config.network().setTimeVariantNetwork(true);
		config.network().setInputFile(testUtils.getPackageInputDirectory() + "network.xml");
		config.network().setChangeEventsInputFile(testUtils.getPackageInputDirectory() + "networkChangeEvents_2016-03-15.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
				
//		LinkImpl link = (LinkImpl) scenario.getNetwork().getLinks().get(Id.createLinkId("36087"));
		TimeVariantLinkImpl link = (TimeVariantLinkImpl) scenario.getNetwork().getLinks().get(Id.createLinkId("36087"));
		
		Assert.assertEquals("Wrong capacity. Check if the flow capacity is given in 'vehicles per hour' 'vehicles per second'.", 4700. / 3600., link.getFlowCapacityPerSec(3 * 3600.), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Wrong capacity (after first network change event).", 0., link.getFlowCapacityPerSec(10 * 3600.), MatsimTestUtils.EPSILON);		
		Assert.assertEquals("Wrong freespeed (after first network change event).", 0.22227, link.getFreespeed(10 * 3600.), MatsimTestUtils.EPSILON);		
	
		Assert.assertEquals("Wrong capacity (after second network change event).", 1.306, link.getFlowCapacityPerSec(16 * 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong freespeed (after second network change event).", 13.88888888888888, link.getFreespeed(16 * 3600.), MatsimTestUtils.EPSILON);
		
	}
		
}
