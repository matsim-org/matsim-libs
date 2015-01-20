/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.pieter.singapore.utils.plans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import others.sergioo.util.dataBase.NoConnectionException;

import java.io.IOException;
import java.sql.SQLException;

import static java.lang.Math.random;

/**
 * Notes:
 * <ul>
 * <li>It is our (kn+mz) intuition that this part of the tutorial is rather
 * out-dated and should either be modernized or deleted.
 * </ul>
 *
 * @author kn after mrieser
 */
public class MyPlansToPlans {

    void run(final String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
        Scenario scenario;
        MatsimRandom.reset(123);
//		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
//		new MatsimNetworkReader(scenario).readFile(args[0]);
//		new MatsimPopulationReader(scenario).readFile(args[0]);
        System.out.println(scenario.getPopulation().getPersons().size());
        new PlanResetter(scenario).run();
//        new PlansSubsampler().run(scenario, args[1], 0.01);
        new PopulationWriter(scenario.getPopulation(),scenario.getNetwork()).write(args[1]);


//		new MatsimFacilitiesReader((ScenarioImpl) scenario).readFile(args[1]);
//		DataBaseAdmin dba = new DataBaseAdmin(new File("data/hitsdb.properties"));
//		Population pop = scenario.getPopulation();
//
//		PlansFilterNoRoute pf = new PlansFilterNoRoute();
//		pf.run(pop,args[2],scenario.getNetwork());

//		PlansAddCarAvailability pca = new PlansAddCarAvailability();
//		pca.run(pop);
//		new PlansSetMode().run(pop);

//		new PlansStripOutTransitPlans().run(pop);
//		PopulationWriter popwriter = new PopulationWriter(pop,
//				scenario.getNetwork());
//		popwriter.write(args[1]);

//		new PlanRouteStripper().run(pop);
//		PopulationWriter popwriter = new PopulationWriter(pop,
//				scenario.getNetwork());
//		popwriter.write(args[1]);

//		new PlansSubsampler().run(scenario, args[1], 0.01);
//		System.out.println("done.");

//		new PlanFindLegDistances(scenario, dba).run();
    }

    public static void main(final String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
        MyPlansToPlans app = new MyPlansToPlans();
        app.run(args);
    }

}

class PlanResetter {
    Config config;
    Population population;


    PlanResetter(Scenario scenario) {
        this.config = scenario.getConfig();
        this.population = scenario.getPopulation();
    }

    public void run() {
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                String mode  = Math.random()>0.1?TransportMode.car:TransportMode.pt;
                double lastDepartureTime = 0;
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity act = (Activity) planElement;
                        String type = act.getType();
                        PlanCalcScoreConfigGroup.ActivityParams params = config.planCalcScore().getActivityParams(type);
                        if (lastDepartureTime > 0) {
                            act.setStartTime(lastDepartureTime + random() *5* 3600);
                        }
                        act.setEndTime(Math.max(0, act.getStartTime()) + random() * 24 * 3600);
                        lastDepartureTime = act.getEndTime();
                    }
                    if (planElement instanceof Leg) {
                        Leg leg = (Leg) planElement;
                        leg.setRoute(null);
                        leg.setDepartureTime(lastDepartureTime);
                        leg.setMode(mode);
                    }
                }
            }
        }
    }
}