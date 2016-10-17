/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.gleich.triphistogram;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Inject;

import playground.jbischoff.analysis.TripHistogram;

/**
 * Tests TripHistogram.class whether its counts of the number of trips is right
 * for the pt tutorial.
 * 
 * @author gleich
 */
public class TripHistogramIT {

	private final static Logger log = Logger.getLogger(TripHistogramIT.class);
	
	public @Rule MatsimTestUtils utils = new MatsimTestUtils();
	
	/*
	 * In the initial population file (of the pt-tutorial) all legs are saved without any route.
	 * Therefore every pt trip is still a single pt leg and it is sufficient to manually count
	 * these pt legs and compare them to the trip count by tripHistogram. Only after routing these 
	 * pt trips are split into multiple pt legs with "pt interaction" activities in between.
	 */
	@Test
	public void ensure_trip_counts_equal_number_of_planned_legs_in_input_population_file() {
		Config config = this.utils.loadConfig("../../examples/scenarios/pt-tutorial/0.config.xml");

		config.vspExperimental().setAbleToOverwritePtInteractionParams(true);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		try {
			Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					bind(TripHistogram.class).asEagerSingleton();
					addControlerListenerBinding().to(TripHistogramTestListener.class).asEagerSingleton();
				}
			});
			controler.run();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.fail("There shouldn't be any exception, but there was ... :-(");
		}
	}
	
}
class TripHistogramTestListener implements IterationEndsListener {
	@Inject TripHistogram tripHistogram;
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		/* 1138: car legs in population file = TripHistogram car */
		Assert.assertEquals(1138, sumIntArray(tripHistogram.getDepartures("car")));

		/* 1090: population file pt = TripHistogram pt + TripHistogram transit_walk */
		Assert.assertEquals(1090, sumIntArray(
				tripHistogram.getDepartures("pt")) + sumIntArray(tripHistogram.getDepartures("transit_walk")));
	}
	
	private int sumIntArray(int[] array){
		int sum = 0;
		for(int i = 0; i < array.length; i++){
			sum += array[i];
		}
		return sum;
	}
	
}
