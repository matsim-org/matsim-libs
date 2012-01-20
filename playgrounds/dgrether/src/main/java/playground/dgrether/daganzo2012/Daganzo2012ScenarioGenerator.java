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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.NetworkUtils;

import playground.dgrether.DgPaths;

/**
 * @author dgrether
 *
 */
public class Daganzo2012ScenarioGenerator {

	private static final Logger log = Logger
			.getLogger(Daganzo2012ScenarioGenerator.class);

	public static String DAGANZO_SVN_DIR = "shared-svn/studies/dgrether/daganzo2012/";

	public static String DAGANZOBASEDIR = DgPaths.REPOS + DAGANZO_SVN_DIR;

	public static final String NETWORK_INPUTFILE = DAGANZOBASEDIR
			+"daganzo_2012_network.xml";

	private static final String PLANS_OUTPUTFILE = DAGANZOBASEDIR
			+ "daganzo_2012_plans.xml";

	private static final String CONFIG_INPUTFILE = DAGANZOBASEDIR
			+ "daganzo_2012_config.xml";

	public static final String SIGNALS_OUTPUTFILE = "daganzo_2012_signal_systems.xml";

	public static final String SIGNALS_CONFIG_OUTPUTFILE = "daganzo_signal_system_configuration.xml";

	public static int iterations = 4000;

	private static final int agents = 5000;

	public static final String runId = "1150";

	public static final String OUTPUTDIR = "/media/data/work/repos/runs-svn/run" + runId + "/";
	
	private ScenarioImpl scenario = null;

	private Config config;

	private boolean isUseReplanning = false;

	private Id getId(int i){
		return this.scenario.createId(Integer.toString(i));
	}
	
	public void createScenario() {
		this.config = ConfigUtils.loadConfig(CONFIG_INPUTFILE);
		config.network().setInputFile(NETWORK_INPUTFILE);
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);

		//TODO plans, signals
		this.createPlans(this.scenario);

		PopulationWriter popWriter = new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork());
		popWriter.writeV5(PLANS_OUTPUTFILE);
		
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

		for (int i = 1; i <= this.agents; i++) {
			PersonImpl p = (PersonImpl) factory.createPerson(scenario.createId(Integer
					.toString(i)));
			homeEndTime+= 1;
			Plan plan = null;
			Plan plan2 = null;
			if (! isUseReplanning){
			  plan = this.createPlan(false, factory, homeEndTime, network);
			  p.addPlan(plan);
        plan2 = this.createPlan(true, factory, homeEndTime, network);
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
    ActivityImpl act1 = (ActivityImpl) factory.createActivityFromLinkId("h", getId(1));
    act1.setEndTime(homeEndTime);
    plan.addActivity(act1);
    // leg to home
    LegImpl leg = (LegImpl) factory.createLeg(TransportMode.car);
    LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(getId(1), getId(7));
    if (useAlternativeRoute) {
      route.setLinkIds(getId(1), NetworkUtils.getLinkIds("2 3 5 6"), getId(7));
    }
    else {
      route.setLinkIds(getId(1), NetworkUtils.getLinkIds("2 4 6"), getId(7));
    }
    leg.setRoute(route);

    plan.addLeg(leg);

    ActivityImpl act2 = (ActivityImpl) factory.createActivityFromLinkId("h", getId(7));
    act2.setLinkId(getId(7));
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
