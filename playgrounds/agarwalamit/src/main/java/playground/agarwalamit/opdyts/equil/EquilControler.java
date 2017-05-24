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

package playground.agarwalamit.opdyts.equil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.OpdytsModalStatsControlerListener;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 22.05.17.
 */


public class EquilControler {

    private static String EQUIL_DIR = "./examples/scenarios/equil-mixedTraffic/";
    private static String OUT_DIR = FileUtils.RUNS_SVN+"opdyts/equil/car,bicycle/ascAnalysis/";
    private static final OpdytsScenario EQUIL_MIXEDTRAFFIC = OpdytsScenario.EQUIL_MIXEDTRAFFIC;

    private static final boolean isPlansRelaxed = false;

    private static final int ascTrials [] = {0,1,2,3,4,5,6,7,8,9,10};
    private static final int avgItrAfterValueOfObjectiveFn [] = {0, 10, 25, 50, 100};

    public static void main(String[] args) {

        if(args.length>0){
            EQUIL_DIR = args[0];
            OUT_DIR = args[1];
        }

        List<String> modes2consider = Arrays.asList("car","bicycle");

        //see an example with detailed explanations -- package opdytsintegration.example.networkparameters.RunNetworkParameters
        Config config = ConfigUtils.loadConfig(EQUIL_DIR+"/config.xml");

        config.plans().setInputFile("plans2000.xml.gz");

        //== default config has limited inputs
        StrategyConfigGroup strategies = config.strategy();
        strategies.clearStrategySettings();

        config.changeMode().setModes( modes2consider.toArray(new String [modes2consider.size()]));
        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.name());
        modeChoice.setWeight(0.1);
        config.strategy().addStrategySettings(modeChoice);

        StrategyConfigGroup.StrategySettings expChangeBeta = new StrategyConfigGroup.StrategySettings();
        expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        expChangeBeta.setWeight(0.9);
        config.strategy().addStrategySettings(expChangeBeta);

        //==

        //== planCalcScore params (initialize will all defaults).
        for ( PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
            params.setTypicalDurationScoreComputation( PlanCalcScoreConfigGroup.TypicalDurationScoreComputation.relative );
        }

        // remove other mode params
        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
        for ( PlanCalcScoreConfigGroup.ModeParams params : planCalcScoreConfigGroup.getModes().values() ) {
            planCalcScoreConfigGroup.removeParameterSet(params);
        }

        PlanCalcScoreConfigGroup.ModeParams mpCar = new PlanCalcScoreConfigGroup.ModeParams("car");
        PlanCalcScoreConfigGroup.ModeParams mpBike = new PlanCalcScoreConfigGroup.ModeParams("bicycle");
        mpBike.setMarginalUtilityOfTraveling(0.);


        planCalcScoreConfigGroup.addModeParams(mpCar);
        planCalcScoreConfigGroup.addModeParams(mpBike);
        //==

        //==
        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.withHoles );
        config.qsim().setUsingFastCapacityUpdate(true);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        //==

        if(! isPlansRelaxed) {

            config.controler().setOutputDirectory(OUT_DIR+"/relaxingPlans/");
            config.controler().setLastIteration(50);
            config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

            Scenario scenarioPlansRelaxor = ScenarioUtils.loadScenario(config);
            // following is taken from KNBerlinControler.prepareScenario(...);
            // modify equil plans:
            double time = 6*3600. ;
            for ( Person person : scenarioPlansRelaxor.getPopulation().getPersons().values() ) {
                Plan plan = person.getSelectedPlan() ;
                Activity activity = (Activity) plan.getPlanElements().get(0) ;
                activity.setEndTime(time);
                time++ ;
            }

            Controler controler = new Controler(scenarioPlansRelaxor);
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addControlerListenerBinding().toInstance(new OpdytsModalStatsControlerListener(modes2consider, new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC)));
                }
            });
            FileUtils.deleteIntermediateIterations(config.controler().getOutputDirectory(),controler.getConfig().controler().getFirstIteration(), controler.getConfig().controler().getLastIteration());
        }

        // set back settings for opdyts
        File file = new File(config.controler().getOutputDirectory()+"/output_plans.xml.gz");
        config.plans().setInputFile(file.getAbsoluteFile().getAbsolutePath());
        config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);

        for (int asc : ascTrials){
            OUT_DIR = OUT_DIR+"/ascRun"+asc+"/";
            config.planCalcScore().getOrCreateModeParams("bicycle").setConstant(asc);
            config.controler().setOutputDirectory(OUT_DIR);
            config.controler().setLastIteration(500);

            Scenario scenario = ScenarioUtils.loadScenario(config);

            DistanceDistribution distanceDistribution = new EquilDistanceDistribution(EQUIL_MIXEDTRAFFIC);
            OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,distanceDistribution);

            Controler controler = new Controler(scenario);
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addControlerListenerBinding().toInstance(stasControlerListner);
                    addControlerListenerBinding().to(KaiAnalysisListener.class);
                }
            });
            controler.run();
        }
    }
}
