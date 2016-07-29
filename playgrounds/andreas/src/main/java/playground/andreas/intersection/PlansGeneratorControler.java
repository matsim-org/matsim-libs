/* *********************************************************************** *
 * project: org.matsim.*
 * QControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.andreas.intersection;

import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansGeneratorControler {

	final private static Logger log = Logger.getLogger(PlansGeneratorControler.class);
	private final Controler controler;

	public PlansGeneratorControler(final Config config) {
		this.controler = new Controler(config);
	}


//	/** Should be overwritten in case of artificial population */
//	@Override
//	protected Population loadPopulation() {
//
////		return generate4wPersons();
//		return generateSimplePlans();
//	}

	private Population generate4wPersons(){

		int numberOfPlans = 1;
		Population pop = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		log.info("  generating plans... ");

		for (int i = 0; i < 314; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(20, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(9, Link.class)), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 948; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(20, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(7, Link.class)), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 196; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(20, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(5, Link.class)), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 56; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(40, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(3, Link.class)), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 192; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(40, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(9, Link.class)), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 185; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(40, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(7, Link.class)), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 170; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(60, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(5, Link.class)), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 799; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(60, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(3, Link.class)), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 147; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(60, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(9, Link.class)), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 150; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(80, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(7, Link.class)), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 166; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(80, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(5, Link.class)), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 341; i++) {
            generatePerson(numberOfPlans, controler.getScenario().getNetwork().getLinks().get(Id.create(80, Link.class)), controler.getScenario().getNetwork().getLinks().get(Id.create(3, Link.class)), pop);
			numberOfPlans++;
		}

		log.info("  generated " + (numberOfPlans - 1) + " plans... ");
		return pop;
	}

	private Population generateSimplePlans(){
		final int agentsPerDest = 1;
		int numberOfPlans = 1;

		Population pop = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		log.info("  generating plans... ");

		LinkedList <Link> fromLinks = new LinkedList<Link>();
		LinkedList <Link> toLinks = new LinkedList<Link>();

        fromLinks.add(controler.getScenario().getNetwork().getLinks().get(Id.create("1", Link.class)));
        fromLinks.add(controler.getScenario().getNetwork().getLinks().get(Id.create("2", Link.class)));

        toLinks.add(controler.getScenario().getNetwork().getLinks().get(Id.create("6", Link.class)));


		for(int i=0; i < 1000; i++){
			for (Link fromLink : fromLinks) {

				for (Link toLink : toLinks) {

					if (!fromLink.equals(toLink)){

						for (int ii = 1; ii <= agentsPerDest; ii++) {
							generatePerson(numberOfPlans, fromLink, toLink, pop);
							numberOfPlans++;
						}
					}
				}
			}

		}

		log.info("  generated " + (numberOfPlans - 1) + " plans... ");
		return pop;
	}


	/** Generates one Person a time */
	private void generatePerson(final int ii, final Link fromLink, final Link toLink, final Population population) {
		Person p = PopulationUtils.getFactory().createPerson(Id.create(String.valueOf(ii), Person.class));
		Plan plan = PopulationUtils.createPlan(p);
		try {
			Activity act1 = PopulationUtils.createAndAddActivityFromCoord(plan, (String) "h", new Coord(100., 100.));
			act1.setLinkId(fromLink.getId());
			act1.setStartTime(0.);
			act1.setEndTime(3 * 60 * 60.);
//			plan.createAct("h", 100., 100., fromLink, 0., 3 * 60 * 60. + 3600 * MatsimRandom.getLocalInstance().nextDouble(), Time.UNDEFINED_TIME, true);
			PopulationUtils.createAndAddLeg( plan, (String) TransportMode.car );
			Activity act2 = PopulationUtils.createAndAddActivityFromCoord(plan, (String) "h", new Coord(200., 200.));
			act2.setLinkId(toLink.getId());
			act2.setStartTime(8 * 60 * 60);

			p.addPlan(plan);
			population.addPerson(p);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static void main(final String[] args) {

		Config config;
		config = ConfigUtils.loadConfig("./src/playground/andreas/intersection/test/data/bottleneck/config.xml");
		final PlansGeneratorControler controler = new PlansGeneratorControler(config);

		controler.run();

	}

	private void run() {
		controler.run();
	}

}
