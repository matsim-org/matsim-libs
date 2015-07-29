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

package org.matsim.contrib.evacuation.run;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.io.ConfigIO;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.contrib.evacuation.scenariogenerator.ScenarioGenerator;
import org.matsim.contrib.evacuation.visualization.OTFVisVisualization;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * This class is part of the GRIPS project(GIS based risk
assessment and incident preparation system)
 * It provides a scenario generation (just like in the test case) and visualizes it in OTFVis.
 * @author jbischoff
 *
 */
public class RunEvacuationExample
{
    private static final String INPUTDIR = "test/input/org/matsim/contrib/evacuation/scenariogenerator/ScenarioGeneratorTest/testScenarioGenerator/";
    
    public static void main(String[] args) 
    {

        
        ArrayList<Id<Link>> closedRoadIDs = new ArrayList<>();
        closedRoadIDs.add(Id.create(156, Link.class));
        closedRoadIDs.add(Id.create(316, Link.class));
        closedRoadIDs.add(Id.create(263, Link.class));
       
    
        
        String gripsFileString = INPUTDIR + "/grips_example_config.xml";
        String matsimConfigFileString =  "outputGrips/config.xml";
        
        System.out.println("grips file:" + gripsFileString);
        System.out.println("matsim config file:" + matsimConfigFileString);
        
        
        File gripsConfigFile = new File(gripsFileString);
        File matsimConfigFile;
        
        Controller controller = new Controller();
        EvacuationConfigModule gcm;
        Config mc;
        
       

        controller.evacuationEvacuationConfig(gripsConfigFile);
        
        gcm = controller.getEvacuationConfigModule();
        gcm.setOutputDir("output/");
        
        //generate matsim network/config
        ScenarioGenerator scengen = new org.matsim.contrib.evacuation.scenariogenerator.ScenarioGenerator(gripsFileString);
        scengen.run();
      
        
        //open matsim scenario config file
        System.out.println("string:" + matsimConfigFileString);
        matsimConfigFile = new File(matsimConfigFileString);
        controller.openMastimConfig(matsimConfigFile);
        
        //open matsim config, set first and last iteration
        mc = controller.getScenario().getConfig();
        mc.setParam("controler", "firstIteration", "0");
        mc.setParam("controler", "lastIteration", "10");
        new ConfigWriter(mc).write(matsimConfigFileString);
        
        //save road closures
        HashMap<Id<Link>, String> roadClosures = new HashMap<Id<Link>, String>();
        for (Id<Link> id : closedRoadIDs)
            roadClosures.put(id, "00:00");
        ConfigIO.saveRoadClosures(controller, roadClosures);

        
        //simulate scenario

        Controler matsimController = new Controler(mc);
        matsimController.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
        matsimController.run();

        
        new OTFVisVisualization("outputGrips/output/output_config.xml.gz", 10).run();;
        
        

    }

}
