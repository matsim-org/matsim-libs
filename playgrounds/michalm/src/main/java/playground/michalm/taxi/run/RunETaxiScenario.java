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

package playground.michalm.taxi.run;

import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.michalm.ev.*;
import playground.michalm.ev.data.*;


public class RunETaxiScenario
{
    public static void run(String configFile, boolean otfvis)
    {
        Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(),
                new OTFVisConfigGroup(), new EvConfigGroup());
        createControler(config, otfvis).run();
    }


    public static Controler createControler(Config config, boolean otfvis)
    {
        Controler controler = RunTaxiScenario.createControler(config, otfvis);
        EvConfigGroup evCfg = EvConfigGroup.get(config);

        EvData evData = new EvDataImpl();
        new ChargerReader(controler.getScenario(), evData).parse(evCfg.getChargerFile());// create xml and DTD
        
        //TODO unfinished
        
        controler.addOverridingModule(new EvModule(evData));
        
        

        return controler;
    }


    public static void main(String[] args)
    {
        String configFile = "./src/main/resources/one_taxi/one_taxi_config.xml";
        //String configFile = "./src/main/resources/mielec_2014_02/config.xml";
        RunETaxiScenario.run(configFile, true);
    }
}
