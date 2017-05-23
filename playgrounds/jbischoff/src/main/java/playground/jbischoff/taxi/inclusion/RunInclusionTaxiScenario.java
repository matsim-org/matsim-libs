/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.inclusion;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.jbischoff.taxi.setup.*;


public class RunInclusionTaxiScenario
{
    public static void run(String configFile, boolean otfvis)
    {
    	
        Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
                new OTFVisConfigGroup());
        createControler(config, otfvis).run();
    
    }
    public static void runMany(String configFile)
    {
    	for (int i = 0; i<=9; i++){
    	
        Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
                new OTFVisConfigGroup());
        config.controler().setOutputDirectory("D:/runs-svn/barrierFreeTaxi/v2/veh250_"+i+"/");
        config.controler().setRunId("veh250_"+i);
        config.plans().setInputFile("itaxi_"+i+".xml.gz");
//        config.plans().setInputFile("itaxi_"+0+".xml.gz");
        DvrpConfigGroup.get(config).setMode(TaxiModule.TAXI_MODE);
        config.qsim().setEndTime(36*3600);
        TaxiConfigGroup taxi = (TaxiConfigGroup) config.getModules().get(TaxiConfigGroup.GROUP_NAME);
        taxi.setTaxisFile("hc_vehicles"+250+".xml.gz");
        createControler(config, false).run();
    	}
    }


    public static Controler createControler(Config config, boolean otfvis)
    {
        config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
        config.checkConsistency();

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new TaxiModule(JbTaxiOptimizerProvider.class));
        
//        controler.addOverridingModule(new JbTaxiModule());

        if (otfvis) {
            controler.addOverridingModule(new OTFVisLiveModule());
        }

        return controler;
    }


    public static void main(String[] args)
    {
//        RunInclusionTaxiScenario.run("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/inclusion/config.xml", false);
        RunInclusionTaxiScenario.runMany("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/inclusion/config.xml");
    }
}
