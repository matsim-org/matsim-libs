/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package commercialtraffic.hannover;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import commercialtraffic.integration.CommercialTrafficConfigGroup;
import commercialtraffic.integration.CommercialTrafficModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

//import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

//import ch.sbb.matsim.routing.pt.raptor.*;


/**
 * @author jbischoff
 */

public class RunHannoverCommercialTrafficExample {
    public static void main(String[] args) {
        String runId = "haj-delivery";

        Config config = ConfigUtils.loadConfig(args[0], new CommercialTrafficConfigGroup());
        config.controler().setOutputDirectory("output\\" + runId);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setRunId(runId);


        Scenario scenario = ScenarioUtils.loadScenario(config);
        adjustPtNetworkCapacity(scenario.getNetwork(), config.qsim().getFlowCapFactor());

        Controler controler = new Controler(scenario);


        // tell the system to use the congested car router for the ride mode:
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });

        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.addOverridingModule(new CommercialTrafficModule());


        controler.run();


    }

    /**
     * this is useful for pt links when only a fraction of the population is simulated, but bus frequency remains the same.
     * Otherwise, pt vehicles may get stuck.
     */
    private static void adjustPtNetworkCapacity(Network network, double flowCapacityFactor) {
        if (flowCapacityFactor < 1.0) {
            for (Link l : network.getLinks().values()) {
                if (l.getAllowedModes().contains(TransportMode.pt)) {
                    l.setCapacity(l.getCapacity() / flowCapacityFactor);
                }
            }
        }
    }
}
