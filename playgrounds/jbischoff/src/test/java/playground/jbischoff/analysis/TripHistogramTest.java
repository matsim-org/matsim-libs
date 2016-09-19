/* *********************************************************************** *
 * project: org.matsim.*
 * LegHistogramTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.jbischoff.analysis;

import java.util.Set;

import org.junit.Rule;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.ControlerListenerManagerImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author jbischoff
 */
public class TripHistogramTest extends MatsimTestCase {

	/**
	 * Tests that different modes of transport are recognized and accounted
	 * accordingly.  Also tests that modes not defined as constants are
	 * handled correctly.
	 */
	public @Rule MatsimTestUtils utils = new MatsimTestUtils();

	public void testDeparturesMiscModes() {

		Config config = this.utils.loadConfig("examples/pt-tutorial/0.config.xml");
		config.controler().setLastIteration(1);
		Controler controler = new Controler(config);
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				addControlerListenerBinding().to(TripHistogramListener.class).asEagerSingleton();
				bind(TripHistogram.class).asEagerSingleton();
			}
		});
		controler.run();
		
	}


}
