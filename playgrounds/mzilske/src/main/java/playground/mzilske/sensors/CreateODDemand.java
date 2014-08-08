/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CreateODDemand.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.sensors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.matrices.Matrix;
import playground.mzilske.cdr.LinkIsZone;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.TrajectoryReRealizerFactory;
import playground.mzilske.cdr.ZoneTracker;
import playground.mzilske.controller.Controller;
import playground.mzilske.controller.ControllerModule;

import java.util.ArrayList;
import java.util.List;

public class CreateODDemand {

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        PlanCalcScoreConfigGroup.ActivityParams sightingParam = new PlanCalcScoreConfigGroup.ActivityParams("sighting");
        sightingParam.setTypicalDuration(30.0 * 60);
        config.planCalcScore().addActivityParams(sightingParam);
        config.planCalcScore().setTraveling_utils_hr(0);
        config.planCalcScore().setPerforming_utils_hr(0);
        config.planCalcScore().setTravelingOther_utils_hr(0);
        config.planCalcScore().setConstantCar(0);
        config.planCalcScore().setMonetaryDistanceCostRateCar(0);
        config.controler().setLastIteration(100);
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(new IdImpl(1));
            stratSets.setModuleName("SelectExpBeta");
            stratSets.setProbability(0.7);
            config.strategy().addStrategySettings(stratSets);
        }
        {
            StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings(new IdImpl(2));
            stratSets.setModuleName("ReRealize");
            stratSets.setProbability(0.3);
            config.strategy().addStrategySettings(stratSets);
        }

        final Matrix matrix = matrix();

        // Antoniou et al, 2000. Switched 8 to 2, I think it is a typo in the paper.
        load(matrix, 2, 7, 250);
        load(matrix, 1, 7, 250);
        load(matrix, 1, 10, 250);
        load(matrix, 2, 4, 250);
        load(matrix, 1, 4, 250);
        load(matrix, 9, 7, 250);


        final Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario).parse(CreateODDemand.class.getResourceAsStream("network.xml"));


        List<Module> modules = new ArrayList<Module>();
        modules.add(new ControllerModule());
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Config.class).toInstance(scenario.getConfig());
                bind(Scenario.class).toInstance(scenario);
                bind(Sightings.class).toProvider(ODSightings.class);
                bind(ZoneTracker.LinkToZoneResolver.class).to(LinkIsZone.class);
                bind(ScoringFunctionFactory.class).to(CharyparNagelScoringFunctionFactory.class);
                MapBinder<String, PlanStrategyFactory> planStrategyFactoryBinder
                        = MapBinder.newMapBinder(binder(), String.class, PlanStrategyFactory.class);
                planStrategyFactoryBinder.addBinding("ReRealize").to(TrajectoryReRealizerFactory.class);
                Multibinder<ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
                bind(Matrix.class).toInstance(matrix);
                controlerListenerBinder.addBinding().toProvider(ODDemandControlerListener.class);
            }
        });

        Injector injector = Guice.createInjector(modules);
        Controller controler = injector.getInstance(Controller.class);
        controler.run();
    }

    private static void load(Matrix matrix, int origin, int destination, int count) {
        matrix.createEntry(new IdImpl(origin), new IdImpl(destination), count);
    }

    private static Matrix matrix() {
        Matrix matrix = new Matrix("wurst", "blubb");
        return matrix;
    }

}
