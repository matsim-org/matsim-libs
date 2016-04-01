/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.run;

import org.junit.Test;
import org.matsim.core.config.*;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


public class RunTaxiScenarioTestIT
{
    @Test
    public void testRunMielecLowDemandLowSupply()
    {
        runMielec("plans_taxi_1.0.xml.gz", "taxis-25.xml");
    }


    @Test
    public void testRunMielecLowDemandHighSupply()
    {
        runMielec("plans_taxi_1.0.xml.gz", "taxis-50.xml");
    }


    @Test
    public void testRunMielecHighDemandLowSupply()
    {
        runMielec("plans_taxi_4.0.xml.gz", "taxis-25.xml");
    }


    @Test
    public void testRunMielecHighDemandHighSupply()
    {
        runMielec("plans_taxi_4.0.xml.gz", "taxis-50.xml");
    }


    private void runMielec(String plansFile, String taxisFile)
    {
        String dir = "./src/main/resources/mielec_2014_02/";
        String configFile = dir + "config.xml";

        TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile, taxiCfg, new OTFVisConfigGroup());
        config.plans().setInputFile(dir + plansFile);
        taxiCfg.setTaxisFile(dir + taxisFile);
        RunTaxiScenario.createControler(config, false).run();
    }
}
