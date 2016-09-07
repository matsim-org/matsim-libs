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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.michalm.ev.*;
import playground.michalm.ev.data.*;
import playground.michalm.ev.data.file.ChargerReader;
import playground.michalm.taxi.data.file.EvrpVehicleReader;
import playground.michalm.taxi.ev.*;


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
        TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
        EvConfigGroup evCfg = EvConfigGroup.get(config);
        config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
        config.checkConsistency();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        TaxiData taxiData = new TaxiData();
        new EvrpVehicleReader(scenario.getNetwork(), taxiData).readFile(taxiCfg.getTaxisFile());
        EvData evData = new EvDataImpl();
        new ChargerReader(scenario.getNetwork(), evData).readFile(evCfg.getChargerFile());
        ETaxiUtils.initEvData(taxiData, evData);

        Controler controler = RunTaxiScenario.createControler(scenario, taxiData, otfvis);
        controler.addOverridingModule(new EvModule(evData));
        controler.addOverridingModule(new DynQSimModule<>(ETaxiQSimProvider.class));

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install()
            {
                addMobsimListenerBinding().toProvider(ETaxiChargerOccupancyTimeProfileCollectorProvider.class);
                addMobsimListenerBinding().toProvider(ETaxiChargerOccupancyXYDataProvider.class);
            }
        });

        return controler;
    }


    public static void main(String[] args)
    {
        //String configFile = "./src/main/resources/one_etaxi/one_etaxi_config.xml";
        String configFile = "../../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/mielec_etaxi_config.xml";
        RunETaxiScenario.run(configFile, false);
    }
}
