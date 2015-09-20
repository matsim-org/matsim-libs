/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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

package playground.dgrether.daganzo2012;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;

/**
 * @author dgrether
 *
 */
public class Daganzo2012ScenarioGenerator {

	private static final Logger log = Logger
			.getLogger(Daganzo2012ScenarioGenerator.class);

	public static String DAGANZO_SVN_DIR = "shared-svn/studies/dgrether/daganzo2012/scenario_2/";

	public static String DAGANZOBASEDIR = DgPaths.REPOS + DAGANZO_SVN_DIR;

	public static final String NETWORK_INPUTFILE = DAGANZOBASEDIR
			+"network22.xml";

	private static final String PLANS_OUTPUTFILE = DAGANZOBASEDIR
			+ "plans_long_route_selected.xml";
//			+ "plans_short_route_selected.xml";
	private static final int agents = 5000;

	private ScenarioImpl scenario = null;

	private Config config;
	
	private boolean isUseReplanning = false;

	public void createScenario() {
		this.config = ConfigUtils.createConfig();
		config.network().setInputFile(NETWORK_INPUTFILE);
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);

		this.createPlans(this.scenario);
		PopulationWriter popWriter = new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork());
		popWriter.writeV5(PLANS_OUTPUTFILE);
		
//		DgNet2Tex net2tex = new DgNet2Tex();
//		net2tex.convert(this.scenario.getNetwork(), "/media/data/work/repos/shared-svn/studies/dgrether/daganzo2012/workplan/network22.xml.tex");
		
		log.info("scenario written!");
	}


	private void createPlans(ScenarioImpl scenario) {
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();
		double firstHomeEndTime =  120.0;
		double homeEndTime = firstHomeEndTime;
//		Link l1 = network.getLinks().get(scenario.createId("1"));
//		Link l7 = network.getLinks().get(scenario.createId("7"));
		PopulationFactory factory = population.getFactory();

		for (int i = 1; i <=  agents; i++) {
			Person p = factory.createPerson(Id.create(i, Person.class));
			if ((i +1) % 2 == 0){
				homeEndTime += 1;
			}
			Plan plan = null;
			Plan plan2 = null;
			if (! this.isUseReplanning){
			  plan = this.createPlan(true, factory, homeEndTime, network);
			  p.addPlan(plan);
        plan2 = this.createPlan(false, factory, homeEndTime, network);
        p.addPlan(plan2);
			}
			else {
			  plan = this.createPlan(false, factory, homeEndTime, network);
			  p.addPlan(plan);
			}

			population.addPerson(p);
		}
	}

	private Plan createPlan(boolean useAlternativeRoute, PopulationFactory factory,
	    double homeEndTime, Network network){
    Plan plan = factory.createPlan();
    homeEndTime+= 1;
    ActivityImpl act1 = (ActivityImpl) factory.createActivityFromLinkId("h", Id.create(1, Link.class));
    act1.setEndTime(homeEndTime);
    plan.addActivity(act1);
    // leg to home
    LegImpl leg = (LegImpl) factory.createLeg(TransportMode.car);
    LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(Id.create(1, Link.class), Id.create(7, Link.class));
    if (useAlternativeRoute) {
      route.setLinkIds(Id.create(1, Link.class), NetworkUtils.getLinkIds("2 3 5 6"), Id.create(7, Link.class));
    }
    else {
      route.setLinkIds(Id.create(1, Link.class), NetworkUtils.getLinkIds("2 4 6"), Id.create(7, Link.class));
    }
    leg.setRoute(route);

    plan.addLeg(leg);

    ActivityImpl act2 = (ActivityImpl) factory.createActivityFromLinkId("h", Id.create(7, Link.class));
    act2.setLinkId(Id.create(7, Link.class));
    plan.addActivity(act2);
    return plan;
	}


	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new Daganzo2012ScenarioGenerator().createScenario();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
