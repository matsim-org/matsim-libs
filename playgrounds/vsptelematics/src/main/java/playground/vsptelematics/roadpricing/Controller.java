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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import playground.vsptelematics.common.IncidentGenerator;
import playground.vsptelematics.common.TelematicsConfigGroup;
import playground.vsptelematics.ha1.RouteTTObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author dgrether
 *
 */
public class Controller {
	private static final Logger log = Logger.getLogger(Controller.class);
	TelematicsConfigGroup telematicsConfigGroup;
	
	public Controller(String[] args){
		Config config = ConfigUtils.loadConfig( args[0]) ;
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		Controler c = new Controler(scenario);
		telematicsConfigGroup = ConfigUtils.addOrGetModule(config, TelematicsConfigGroup.GROUPNAME, TelematicsConfigGroup.class);
		c.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		c.getConfig().controler().setCreateGraphs(false);
        addListener(c);
		c.run();
	}
	
	
	private void addListener(Controler c) {
        c.setModules(new ControlerDefaultsWithRoadPricingModule());
        c.addOverridingModule(new AbstractModule() {
			  @Override
			  public void install() {
				  addControlerListenerBinding().to(RouteTTObserver.class);
				  if (telematicsConfigGroup.getUsePredictedTravelTimes()) {					  
					  addControlerListenerBinding().toInstance(new TollBehaviour());
				  }
				  if (getConfig().network().isTimeVariantNetwork()) {
					  addControlerListenerBinding().to(IncidentGenerator.class);
				  }
			  }
		});
	}
	
	
	private static class TollBehaviour implements BeforeMobsimListener {
		private double prediction1;
		private double prediction2;
		private double tollRoute1;
		private double tollRoute2;
		private Id<Link> id1 = Id.create("1", Link.class);
		private Id<Link> id6 = Id.create("6", Link.class);
		private Id<Link> id2 = Id.create("2", Link.class);
		private Id<Link> id3 = Id.create("3", Link.class);
		private Id<Link> id4 = Id.create("4", Link.class);
		private Id<Link> id5 = Id.create("5", Link.class);
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
			MatsimServices con = event.getServices();
			prediction1 = Double.parseDouble(con.getScenario().getConfig().getParam("telematics", "predictedTravelTimeRoute1"));
			prediction2 = Double.parseDouble(con.getScenario().getConfig().getParam("telematics", "predictedTravelTimeRoute2"));

			double time = 18.0 * 3600.0;
			RoadPricingSchemeImpl roadPricingScheme = (RoadPricingSchemeImpl) con.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
			tollRoute1 = roadPricingScheme.getLinkCostInfo(Id.create("2", Link.class), time , null, null).amount;
			tollRoute2 = roadPricingScheme.getLinkCostInfo(Id.create("3", Link.class), time, null, null).amount;
//			log.error("using prediction1: " + prediction1 + " prediction2: " + prediction2 + " toll1: " + tollRoute1 + " toll2: " + tollRoute2);
		
			Population pop = con.getScenario().getPopulation();
			for (Person person : pop.getPersons().values()){
				double vot = this.getVoT(person);
				double score1 = (-vot * prediction1/3600.0) - tollRoute1;
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