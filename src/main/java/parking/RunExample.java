/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package parking;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by amit on 13.02.18.
 */

public class RunExample {

    public static void main(String[] args) {

        // removed cadyts config group from config.
        String configFile = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/input/vw202.0.01.output_config.xml";
        String outputDir = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/output/";
        String shapeFile = "../../repos/shared-svn/projects/vw_rufbus/projekt2/parking/shp/parking-zones.shp";

        Config config = ConfigUtils.loadConfig(configFile);
        config.network().setInputFile("../example_scenario/vw202.0.01/vw202.0.01.output_network.xml.gz");
        config.plans().setInputFile("../example_scenario/vw202.0.01/vw202.0.01.output_plans.xml.gz");
//        config.plans().setInputFile("1agent.xml");
        config.counts().setInputFile("../example_scenario/vw202.0.01/vw202.0.01.output_counts.xml.gz");
        config.transit().setTransitScheduleFile("../example_scenario/vw202.0.01/vw202.0.01.output_transitSchedule.xml.gz");
        config.transit().setVehiclesFile("../example_scenario/vw202.0.01/vw202.0.01.output_transitVehicles.xml.gz");

        config.controler().setOutputDirectory(outputDir);

        //parking related settings
        config.plansCalcRoute().setInsertingAccessEgressWalk(true);
        PlanCalcScoreConfigGroup.ActivityParams carParking = new PlanCalcScoreConfigGroup.ActivityParams("car parkingSearch");
        carParking.setScoringThisActivityAtAll(false);
        config.planCalcScore().addActivityParams(carParking);
        //

        Scenario scenario = ScenarioUtils.loadScenario(config);
        ZoneToLinks zoneToLinks =  new ZoneToLinks( shapeFile, "NO", scenario.getNetwork() );

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(ZoneToLinks.class).toInstance(zoneToLinks);
                addRoutingModuleBinding(TransportMode.car).toProvider(new ParkingSearchRoutingModuleProvider(TransportMode.car));
            }
        });

        controler.run();
    }

}
