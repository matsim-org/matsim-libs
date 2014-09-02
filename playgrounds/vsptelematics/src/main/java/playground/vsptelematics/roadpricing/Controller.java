/* *********************************************************************** *
 * project: org.matsim.*
 * Controller
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
package playground.vsptelematics.roadpricing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.roadpricing.RoadPricing;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import playground.vsptelematics.common.IncidentGenerator;
import playground.vsptelematics.ha1.RouteTTObserver;


/**
 * @author dgrether
 *
 */
public class Controller {

	
	private static final Logger log = Logger.getLogger(Controller.class);
	

	public Controller(String[] args){
		Controler c = new Controler(args);
		c.setOverwriteFiles(true);
		c.setCreateGraphs(false);
		addListener(c);
		c.run();
	}
	
	private void addListener(Controler c){
		c.addControlerListener(new RoadPricing());
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				Controler con = event.getControler();
				final RouteTTObserver observer = new RouteTTObserver(con.getControlerIO().getOutputFilename("routeTravelTimes.txt"));
				con.addControlerListener(observer);
				con.getEvents().addHandler(observer);
                if (ConfigUtils.addOrGetModule(event.getControler().getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).isUsingRoadpricing()) {
					con.addControlerListener(new TollBehaviour());
				}
				if (con.getScenario().getConfig().network().isTimeVariantNetwork()){
					IncidentGenerator generator = new IncidentGenerator(con.getScenario().getConfig().getParam("telematics", "incidentsFile"), con.getScenario().getNetwork());
					con.addControlerListener(generator);
				}
			}
			}
		);
	}

	
	private static class TollBehaviour implements BeforeMobsimListener {
		private double prediction1;
		private double prediction2;
		private double tollRoute1;
		private double tollRoute2;
		private Id id1 = new IdImpl("1");
		private Id id6 = new IdImpl("6");
		private Id id2 = new IdImpl("2");
		private Id id3 = new IdImpl("3");
		private Id id4 = new IdImpl("4");
		private Id id5 = new IdImpl("5");
		private List<Id<Link>> route1Ids = new ArrayList<Id<Link>>();
		private List<Id<Link>> route2Ids = new ArrayList<Id<Link>>();
		private Random votRandom = MatsimRandom.getLocalInstance();
		private Random logitRandom = MatsimRandom.getLocalInstance();
		
		public TollBehaviour() {
			this.route1Ids.add(id2);
			this.route1Ids.add(id4);
			this.route2Ids.add(id3);
			this.route2Ids.add(id5);
		}

		private double getVoT(Person p){
			return votRandom.nextDouble() * 10.0;
		}
		
		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			Controler con = event.getControler();
			prediction1 = Double.parseDouble(con.getScenario().getConfig().getParam("telematics", "predictedTravelTimeRoute1"));
			prediction2 = Double.parseDouble(con.getScenario().getConfig().getParam("telematics", "predictedTravelTimeRoute2"));

			double time = 18.0 * 3600.0;
			RoadPricingSchemeImpl roadPricingScheme = (RoadPricingSchemeImpl) con.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
			tollRoute1 = roadPricingScheme.getLinkCostInfo(new IdImpl("2"), time , null, null).amount;
			tollRoute2 = roadPricingScheme.getLinkCostInfo(new IdImpl("3"), time, null, null).amount;
//			log.error("using prediction1: " + prediction1 + " prediction2: " + prediction2 + " toll1: " + tollRoute1 + " toll2: " + tollRoute2);
		
			Population pop = con.getScenario().getPopulation();
			for (Person person : pop.getPersons().values()){
				double vot = this.getVoT(person);
				double score1 = (- vot * prediction1/3600.0) - tollRoute1;
				double score2 = (-vot * prediction2/3600.0) - tollRoute2;
//				log.error("score 1: " + score1 + " score2: " + score2);
				double exp1 = Math.exp(2.0*score1);
				double exp2 = Math.exp(2.0*score2);
				double expSum = exp1 + exp2;
				double p1 = exp1/expSum;
				double p2 = exp2/expSum;
				double logitRand = this.logitRandom.nextDouble();
				Leg leg = (Leg) person.getPlans().get(0).getPlanElements().get(1);

				if (((NetworkRoute)leg.getRoute()).getLinkIds().get(0).equals(id2)){ //Route 1 is selected
					if (logitRand < p2) {
						((NetworkRoute)leg.getRoute()).setLinkIds(id1, route2Ids , id6);
//						log.error("  route 2");
					}
				}
				else {
					if (logitRand < p1) {
						((NetworkRoute)leg.getRoute()).setLinkIds(id1, route1Ids , id6);
//						log.error("  route 1");
					}
				}
			}
			
		}
	};
	
	public static void main(String[] args) {
		new Controller(args);
	}

	
}
