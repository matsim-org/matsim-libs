/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAddTypicalDurationsToDesiresTest.java
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

package playground.meisterk.org.matsim.population.algorithms;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.population.BasicLeg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.ActImpl;
import org.matsim.population.Desires;
import org.matsim.population.LegImpl;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationImpl;
import org.matsim.testcases.MatsimTestCase;

public class PersonAddTypicalDurationsToDesiresTest extends MatsimTestCase {

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	private static Logger log = Logger.getLogger(PlanAnalyzeTourModeChoiceSetTest.class);
	
	private final String HOME = "home";
	private final String FROBNICATE = "frobnicate";
	private final String REFROBNICATE = "refrobnicate";
	
	
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(PersonAddTypicalDurationsToDesiresTest.CONFIGFILE);
	}

	public void testRunPerson() {
		
		// load data
		log.info("Reading network xml file...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		Person person = new PersonImpl(new IdImpl("123"));
		
		Desires initialDesires = person.createDesires("testDesires");
		initialDesires.putActivityDuration(HOME, 16.0 * 3600);
		initialDesires.putActivityDuration(FROBNICATE, 4.0 * 3600);
		initialDesires.putActivityDuration(REFROBNICATE, 4.0 * 3600);
		
		Plan plan = person.createPlan(true);
		plan.addAct(new ActImpl(HOME, network.getLink("1")));
		plan.addLeg(new LegImpl(BasicLeg.Mode.undefined));
		plan.addAct(new ActImpl(FROBNICATE, network.getLink("1")));
		plan.addLeg(new LegImpl(BasicLeg.Mode.undefined));
		plan.addAct(new ActImpl(REFROBNICATE, network.getLink("1")));
		plan.addLeg(new LegImpl(BasicLeg.Mode.undefined));
		plan.addAct(new ActImpl(HOME, network.getLink("1")));
		plan.addLeg(new LegImpl(BasicLeg.Mode.undefined));
		plan.addAct(new ActImpl(REFROBNICATE, network.getLink("1")));
		plan.addLeg(new LegImpl(BasicLeg.Mode.undefined));
		plan.addAct(new ActImpl(REFROBNICATE, network.getLink("1")));
		plan.addLeg(new LegImpl(BasicLeg.Mode.undefined));
		plan.addAct(new ActImpl(FROBNICATE, network.getLink("1")));
		plan.addLeg(new LegImpl(BasicLeg.Mode.undefined));
		plan.addAct(new ActImpl(HOME, network.getLink("1")));

		Population pop = new PopulationImpl();
		pop.addPerson(person);
		
		PersonAddTypicalDurationsToDesires testee = new PersonAddTypicalDurationsToDesires();
		testee.run(pop);
		
		Desires expectedDesires = person.getDesires();
		assertEquals(6, expectedDesires.getActivityDurations().size());
		assertEquals(2.0 * 3600, expectedDesires.getActivityDuration(FROBNICATE + PersonAddTypicalDurationsToDesires.APPENDIX));
		assertEquals((4.0 / 3) * 3600, expectedDesires.getActivityDuration(REFROBNICATE + PersonAddTypicalDurationsToDesires.APPENDIX));
		// TODO do we want home to be split into equal parts, too?
		// "home" is a special type of activity, it implicitly has two meanings
		// - anchor of the daily activity plan ("sleep")
		// - intermediate activity
		// a solution is to have two home-type activity types
		// - something like "home-anchor" with a long minimal typical duration, say 9 hours
		// - other "home", which share the rest of home-activity budget
		assertEquals(8.0 * 3600, expectedDesires.getActivityDuration(HOME  + PersonAddTypicalDurationsToDesires.APPENDIX));
	}

}
