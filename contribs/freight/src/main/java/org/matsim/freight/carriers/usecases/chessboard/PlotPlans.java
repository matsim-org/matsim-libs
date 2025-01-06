/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.chessboard;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;

final class PlotPlans {

    public static void main(String[] args) {
        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input/usecases/chessboard/network/grid9x9.xml");

        CarrierVehicleTypes types = new CarrierVehicleTypes();
        new CarrierVehicleTypeReader(types).readFile("input/usecases/chessboard/freight/vehicleTypes.xml");

        final Carriers carriers = new Carriers();
//		new CarrierPlanXmlReader(carriers).read("input/usecases/chessboard/freight/singleCarrierTwentyActivities.xml");
        new CarrierPlanXmlReader(carriers, types ).readFile("output/ITERS/it.140/140.carrierPlans.xml" );

        final Carrier carrier = carriers.getCarriers().get(Id.create("carrier1",Carrier.class));
        VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scenario.getNetwork());
        VehicleRoutingProblem vrp = vrpBuilder.build();

        VehicleRoutingProblemSolution solution = MatsimJspritFactory.createSolution(carrier.getSelectedPlan(), vrp);

        new Plotter(vrp,solution.getRoutes()).plot("output/plot_140", "carrier1");

    }

}
