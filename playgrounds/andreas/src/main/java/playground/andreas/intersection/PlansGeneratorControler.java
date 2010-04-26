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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class PlansGeneratorControler extends Controler {

	final private static Logger log = Logger.getLogger(PlansGeneratorControler.class);

	public PlansGeneratorControler(final Config config) {
		super(config);
	}

	@Override
	protected void runMobSim() {

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
		Population pop = new ScenarioImpl().getPopulation();
		log.info("  generating plans... ");

		for (int i = 0; i < 314; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("20")), this.network.getLinks().get(new IdImpl("9")), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 948; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("20")), this.network.getLinks().get(new IdImpl("7")), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 196; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("20")), this.network.getLinks().get(new IdImpl("5")), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 56; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("40")), this.network.getLinks().get(new IdImpl("3")), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 192; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("40")), this.network.getLinks().get(new IdImpl("9")), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 185; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("40")), this.network.getLinks().get(new IdImpl("7")), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 170; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("60")), this.network.getLinks().get(new IdImpl("5")), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 799; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("60")), this.network.getLinks().get(new IdImpl("3")), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 147; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("60")), this.network.getLinks().get(new IdImpl("9")), pop);
			numberOfPlans++;
		}

		for (int i = 0; i < 150; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("80")), this.network.getLinks().get(new IdImpl("7")), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 166; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("80")), this.network.getLinks().get(new IdImpl("5")), pop);
			numberOfPlans++;
		}
		for (int i = 0; i < 341; i++) {
			generatePerson(numberOfPlans, this.network.getLinks().get(new IdImpl("80")), this.network.getLinks().get(new IdImpl("3")), pop);
			numberOfPlans++;
		}

		log.info("  generated " + (numberOfPlans - 1) + " plans... ");
		return pop;
	}

	private Population generateSimplePlans(){
		final int agentsPerDest = 1;
		int numberOfPlans = 1;

		Population pop = new ScenarioImpl().getPopulation();
		log.info("  generating plans... ");

		LinkedList <LinkImpl> fromLinks = new LinkedList<LinkImpl>();
		LinkedList <LinkImpl> toLinks = new LinkedList<LinkImpl>();

		fromLinks.add(this.network.getLinks().get(new IdImpl("1")));
		fromLinks.add(this.network.getLinks().get(new IdImpl("2")));

		toLinks.add(this.network.getLinks().get(new IdImpl("6")));


		for(int i=0; i < 1000; i++){
			for (LinkImpl fromLink : fromLinks) {

				for (LinkImpl toLink : toLinks) {

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
	private void generatePerson(final int ii, final LinkImpl fromLink, final LinkImpl toLink, final Population population) {
		PersonImpl p = new PersonImpl(new IdImpl(String.valueOf(ii)));
		PlanImpl plan = new org.matsim.core.population.PlanImpl(p);
		try {
			ActivityImpl act1 = plan.createAndAddActivity("h", new CoordImpl(100., 100.));
			act1.setLinkId(fromLink.getId());
			act1.setStartTime(0.);
			act1.setEndTime(3 * 60 * 60.);
//			plan.createAct("h", 100., 100., fromLink, 0., 3 * 60 * 60. + 3600 * MatsimRandom.getLocalInstance().nextDouble(), Time.UNDEFINED_TIME, true);
			plan.createAndAddLeg(TransportMode.car);
			ActivityImpl act2 = plan.createAndAddActivity("h", new CoordImpl(200., 200.));
			act2.setLinkId(toLink.getId());
			act2.setStartTime(8 * 60 * 60);

			p.addPlan(plan);
			population.addPerson(p);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static void main(final String[] args) {

		Config config = Gbl.createConfig(new String[] { "./src/playground/andreas/intersection/test/data/bottleneck/config.xml" });

		final PlansGeneratorControler controler = new PlansGeneratorControler(config);
		controler.setOverwriteFiles(true);
		controler.setWriteEventsInterval(1);

		controler.run();
	}

}
