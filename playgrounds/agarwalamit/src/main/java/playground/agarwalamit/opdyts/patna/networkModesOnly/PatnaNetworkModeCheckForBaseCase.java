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

package playground.agarwalamit.opdyts.patna.networkModesOnly;

import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.opdyts.analysis.OpdytsModalStatsControlerListener;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 12.06.17.
 */


public class PatnaNetworkModeCheckForBaseCase {

    private static final Logger LOGGER = Logger.getLogger(PatnaNetworkModeCheckForBaseCase.class);

    public static void main(String[] args) {

        String configFile = args[0];
        String outDir = args[1];
        String relaxedPlans = args[2];

        double ascBike = Double.valueOf(args[3]);
        double ascMotorbike = Double.valueOf(args[4]);

        LOGGER.info("asc for bike is set to "+ ascBike);
        LOGGER.info("asc for motorbike is set to "+ ascMotorbike);

        Config config = ConfigUtils.loadConfig(configFile);
        config.plans().setInputFile(relaxedPlans);
        config.controler().setOutputDirectory(outDir);
        config.planCalcScore().getModes().get("bike").setConstant(ascBike);
        config.planCalcScore().getModes().get("motorbike").setConstant(ascMotorbike);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        List<String> modes2consider = Arrays.asList("car","bike","motorbike");

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

                this.addControlerListenerBinding().toInstance(new OpdytsModalStatsControlerListener(modes2consider, new PatnaNetworkModesOneBinDistanceDistribution(
                        OpdytsScenario.PATNA_1Pct)));
            }
        });

        controler.run();

        // delete unnecessary iterations folder here.
        int firstIt = controler.getConfig().controler().getFirstIteration();
        int lastIt = controler.getConfig().controler().getLastIteration();
        FileUtils.deleteIntermediateIterations(scenario.getConfig().controler().getOutputDirectory(),firstIt,lastIt);
    }
}
