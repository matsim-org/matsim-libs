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
package playground.ikaddoura.analysis.vtts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author ikaddoura
 */

public class VTTSHandlerTest {
		
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void test1() {
		
		Config config = ConfigUtils.createConfig();	
		config.planCalcScore().setPerforming_utils_hr(6.0);
		final double traveling = -4.0;
		config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		config.planCalcScore().setMarginalUtilityOfMoney(1.0);
		config.planCalcScore().setEarlyDeparture_utils_hr(0.);
		config.planCalcScore().setLateArrival_utils_hr(-18.);
		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0.);
		
		ActivityParams actParams1 = new ActivityParams("home");
		actParams1.setMinimalDuration(8. * 3600);
		actParams1.setPriority(1.0);
		actParams1.setScoringThisActivityAtAll(true);
		actParams1.setTypicalDuration(12. * 3600);
		actParams1.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		
		ActivityParams actParams2 = new ActivityParams("shop_other");
		actParams2.setClosingTime(21. * 3600);
		actParams2.setLatestStartTime(19. * 3600);
		actParams2.setMinimalDuration(1. * 3600);
		actParams2.setOpeningTime(9.5 * 3600);
		actParams2.setPriority(1.0);
		actParams2.setScoringThisActivityAtAll(true);
		actParams2.setTypicalDuration(4. * 3600);
		actParams2.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		
		ActivityParams actParams3 = new ActivityParams("shop_daily");
		actParams3.setClosingTime(21. * 3600);
		actParams3.setLatestStartTime(20. * 3600);
		actParams3.setMinimalDuration(0.5 * 3600);
		actParams3.setOpeningTime(7.5 * 3600);
		actParams3.setPriority(1.0);
		actParams3.setScoringThisActivityAtAll(true);
		actParams3.setTypicalDuration(1. * 3600);
		actParams3.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		
		ActivityParams actParams4 = new ActivityParams("leisure");
		actParams4.setClosingTime(23.5 * 3600);
		actParams4.setLatestStartTime(21. * 3600);
		actParams4.setMinimalDuration(1.0 * 3600);
		actParams4.setOpeningTime(6. * 3600);
		actParams4.setPriority(1.0);
		actParams4.setScoringThisActivityAtAll(true);
		actParams4.setTypicalDuration(4. * 3600);
		actParams4.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
		
		config.planCalcScore().addActivityParams(actParams1);
		config.planCalcScore().addActivityParams(actParams2);
		config.planCalcScore().addActivityParams(actParams3);
		config.planCalcScore().addActivityParams(actParams4);

		String populationFile = null;
		String networkFile = null;
		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		VTTSHandler vttsHandler = new VTTSHandler(scenario);
		events.addHandler(vttsHandler);
						
		String eventsFile = testUtils.getInputDirectory() + "100.events.b1240739.xml";

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		vttsHandler.computeFinalVTTS();
				
		vttsHandler.printVTTS(testUtils.getOutputDirectory() + "VTTS.csv");
		vttsHandler.printAvgVTTSperPerson(testUtils.getOutputDirectory() + "avgVTTS.csv"); 
		
		Assert.assertEquals("wrong VTTS", 15.4866868060989, vttsHandler.getPersonId2TripNr2VTTSh().get(Id.createPersonId("b1240739")).get(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong VTTS", 79.0949637642268, vttsHandler.getPersonId2TripNr2VTTSh().get(Id.createPersonId("b1240739")).get(2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong VTTS", 17.805855345339, vttsHandler.getPersonId2TripNr2VTTSh().get(Id.createPersonId("b1240739")).get(3), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong VTTS", "walk", vttsHandler.getPersonId2TripNr2Mode().get(Id.createPersonId("b1240739")).get(2));
		Assert.assertEquals("wrong VTTS", "car", vttsHandler.getPersonId2TripNr2Mode().get(Id.createPersonId("b1240739")).get(3));
	}
}
