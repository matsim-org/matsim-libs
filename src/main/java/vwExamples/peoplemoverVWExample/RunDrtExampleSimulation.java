
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

package vwExamples.peoplemoverVWExample;

import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


/**
 * @author steffen.axer@volkswagen.de
 */

public class RunDrtExampleSimulation {

    //Class to create the controller
    public static Controler createControler(Config config, boolean otfvis) {
        config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
        config.checkConsistency();
        return DrtControlerCreator.createControler(config, otfvis);
    }

    public static void main(String[] args) {

        //Define the path to the config file and enable / disable otfvis
        final Config config = ConfigUtils.loadConfig("D:/Axer/MatsimDataStore/DAI-Package/config.xml", new DrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());

        //Disable otfvis visulization interface
        boolean otfvis = false;

        //Overwrite existing or add configuration parameters in the matsim config file

        //Where to write output
        config.controler().setOutputDirectory("C:/Users/VWBIDGN/Desktop/MATSimOuput");

        //Number of iterations, counted from 0!
        config.controler().setLastIteration(4);

        //When to write output events: 1 --> every iteration is written
        config.controler().setWriteEventsInterval(1);

        //When to write output plans: 1 --> every iteration is written
        config.controler().setWritePlansInterval(1);

        //Define which input plans should be used
        config.plans().setInputFile("D:/Axer/MatsimDataStore/DAI-Package/inputplans/run124.100.output_plans_DRT0.1.xml.gz");

        //Add config parameter to DrtConfigGroup
        DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
        //Take only the kNearestVehicles within the DRT dispatching


        //Initialize the controller
        Controler controler = createControler(config, otfvis);

        //Run the simulation
        controler.run();


    }
}

