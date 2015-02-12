/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.matrixbasedptrouter.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterFactoryImpl;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * sets up a Matsim Scenario using example matrix based pt routing data
 */
/**
 * @author jbischoff
 *
 */

public class RunMatrixBasedPTRouterExample
{

    public static void main(String[] args)
    {
        //load config - check the config file for required PT Matrix input file data
        String path = "src/main/resources/example/";
        Config config = ConfigUtils.loadConfig(path+"example_config.xml", new MatrixBasedPtRouterConfigGroup());
        
        //fetching relevant groups ot of the config
        MatrixBasedPtRouterConfigGroup mbpcg = (MatrixBasedPtRouterConfigGroup) config.getModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME);
        PlansCalcRouteConfigGroup plansCalcRoute = config.plansCalcRoute();
        

        //setting up scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Bounding Box for PT Matrix - may be scaled down to a smaller area
        BoundingBox nbb = BoundingBox.createBoundingBox(scenario.getNetwork());
        // setting up PT Matrix
        PtMatrix ptMatrix = PtMatrix.createPtMatrix(plansCalcRoute, nbb, mbpcg);
        
        //and finally setting up the controler
        Controler controler = new Controler(config);
        // setting up routing 
        controler.setTripRouterFactory( new MatrixBasedPtRouterFactoryImpl(controler.getScenario(), ptMatrix) ); // the car and pt router
        
        controler.run();
        

        
        
        
    }

}
