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

package playground.agarwalamit.opdyts.patna;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.opdyts.OpdytsModalStatsControlerListener;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 05.04.17.
 */


public class PatnaTravelTimeCalculatorReplacementTest {

    private static final String configDir = FileUtils.RUNS_SVN+"/patnaIndia/run111/opdyts/input/";

    private static String configFile;
    private static String OUT_DIR = FileUtils.RUNS_SVN+"/patnaIndia/run111/opdyts/outputTravelTime/";

    private static Map<String, TravelTime> modalTravelTimeForReplacement = new HashMap<>();

    public static void main(String[] args) {
        configFile = configDir+"/config_urban_1pct.xml";

        //=========== 1 ==================
        // run for 20 itertaions

        {
            Config config= ConfigUtils.loadConfig(configFile);
            config.linkStats().setAverageLinkStatsOverIterations(1);
            config.controler().setOutputDirectory(OUT_DIR+"/noReplacementOfTravelTime/");
            config.controler().setFirstIteration( 0 );
            config.controler().setLastIteration( 20 );
            config.controler().setCreateGraphs(true);
            config.strategy().setFractionOfIterationsToDisableInnovation(1); // this is must
            Controler controler = getControler(config);
            controler.run();
        }

        //=========== 2 ==================
        // run for 10 itertaions; replace travel times; run again for 10 iterations

        {
            modalTravelTimeForReplacement.clear();
            Config config1= ConfigUtils.loadConfig(configFile);
            config1.linkStats().setAverageLinkStatsOverIterations(1);
            config1.controler().setOutputDirectory(OUT_DIR+"/beforeReplacementOfTravelTime/");
            config1.controler().setFirstIteration( 0 );
            config1.controler().setLastIteration( 10 );
            config1.controler().setCreateGraphs(true);
            config1.strategy().setFractionOfIterationsToDisableInnovation(1); // this is must
            Controler controler1 = getControler(config1);
            controler1.run();

            Config config2= ConfigUtils.loadConfig(configFile);
            config2.linkStats().setAverageLinkStatsOverIterations(1);
            config2.controler().setOutputDirectory(OUT_DIR+"/afterReplacementOfTravelTime2/");
            config2.controler().setFirstIteration( 10 );
            config2.controler().setLastIteration( 20 );
            config2.controler().setCreateGraphs(true);
            config2.strategy().setFractionOfIterationsToDisableInnovation(1); // this is must

            config2.plans().setInputFile( config1.controler().getOutputDirectory()+"/output_plans.xml.gz" );

            Controler controler2 = getControler(config2);

            controler2.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {

                    modalTravelTimeForReplacement.entrySet().forEach(
                            entry -> {
                                addTravelTimeBinding(entry.getKey()).toInstance(entry.getValue());
                            }
                    );
                }
            });

            controler2.run();
        }
    }

    private static Controler getControler(Config config){

        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Set<String> modes2consider = new HashSet<>();
        modes2consider.add("car");
        modes2consider.add("bike");
        modes2consider.add("motorbike");
        modes2consider.add("pt");
        modes2consider.add("walk");

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // add here whatever should be attached to matsim controler

                // some stats
                addControlerListenerBinding().to(KaiAnalysisListener.class);

                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

                this.addControlerListenerBinding().toInstance(new OpdytsModalStatsControlerListener(modes2consider, new PatnaOneBinDistanceDistribution(
                        OpdytsScenario.PATNA_1Pct)));
            }
        });

        // adding pt fare system based on distance
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().to(PtFareEventHandler.class);
            }
        });
        // for above make sure that util_dist and monetary dist rate for pt are zero.
        PlanCalcScoreConfigGroup.ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
        mp.setMarginalUtilityOfDistance(0.0);
        mp.setMonetaryDistanceRate(0.0);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addControlerListenerBinding().to(MyControlerListener.class);
            }
        });

        return controler;
    }

    private static class MyControlerListener implements ShutdownListener, IterationStartsListener, StartupListener {
        @Inject
        Map<String, TravelTime> modalTravelTimes ;

        @Override
        public void notifyShutdown(ShutdownEvent event) {
            modalTravelTimeForReplacement.putAll( modalTravelTimes );
        }

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            MatsimRandom.reset();
        }

        @Override
        public void notifyStartup(StartupEvent event) {
            MatsimRandom.reset();
        }
    }
}
