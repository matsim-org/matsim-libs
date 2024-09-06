/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Example script that shows how to use the extensions developed by SBB (Swiss Federal Railway) included in this repository.
 *
 * @author mrieser / SBB
 */
public class RunSBBExtension {

    private static final  Logger log = LogManager.getLogger(RunSBBExtension.class);

    public static void main(String[] args) {
        String configFilename = args[0];
        Config config = ConfigUtils.loadConfig(configFilename);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);

        // To use the deterministic pt simulation (Part 1 of 2):
        controler.addOverridingModule(new SBBTransitModule());

        // To use the fast pt router (Part 1 of 1)
        controler.addOverridingModule(new SwissRailRaptorModule());

        // To use the deterministic pt simulation (Part 2 of 2):
        controler.configureQSimComponents(components -> {
            new SBBTransitEngineQSimModule().configure(components);

            // if you have other extensions that provide QSim components, call their configure-method here
        });

        controler.run();
    }

}
