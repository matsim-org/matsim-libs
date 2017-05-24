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

import java.io.File;
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
import playground.agarwalamit.opdyts.DistanceDistribution;
import playground.agarwalamit.opdyts.OpdytsModalStatsControlerListener;
import playground.agarwalamit.opdyts.OpdytsScenario;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 22.05.17.
 */

public class PatnaBaseCaseControler {

    public static final Logger LOGGER = Logger.getLogger(PatnaBaseCaseControler.class);

    private enum PatnaBestDecisionVariable {
        REPORTED_BEST, // (from GF) reported as best overall decision variable in opdyts.log
        BEST // (from KN, AA) the decision variable just before the reported best overall decision variable
    }

    private static final OpdytsScenario OPDYTS_SCENARIO_PATNA = OpdytsScenario.PATNA_1Pct;
    private static String baseDir = FileUtils.RUNS_SVN+"/opdyts/patna/output_networkModes/comparison/oldCycle/baseCase/";
    private static String configFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_networkModes/variance_1_relaxedInputs/output_config.xml.gz";

    // cases to run
    private static int [] plansFileIndexes = {0,1};
    private static int [] firstIterations = {0,1,2};
    private static PatnaBestDecisionVariable patnaBestDecisionVariable;

    public static void main(String[] args) {

        boolean isRunningOnCluster = false;
        if (args.length > 0) isRunningOnCluster = true;

        if (isRunningOnCluster ) {
            configFile = args[0];
            baseDir = args[1];
            patnaBestDecisionVariable = PatnaBestDecisionVariable.valueOf(args[2]);
        }

        int firstIteration = 0;
        int lastIteration = 2;

        Config config = ConfigUtils.loadConfig(configFile);
        String tempOutDir = baseDir+"/"+patnaBestDecisionVariable+"/plainRun2GetPlansFile/";
        if (! new File(tempOutDir).exists() ) new File(tempOutDir).mkdirs();
        config.controler().setOutputDirectory(tempOutDir);
        config.controler().setFirstIteration(firstIteration);
        config.controler().setLastIteration(lastIteration);

        if(patnaBestDecisionVariable.equals(PatnaBestDecisionVariable.REPORTED_BEST)) {
            config.planCalcScore().getOrCreateModeParams("bike").setConstant(0.94);
            config.planCalcScore().getOrCreateModeParams("motorbike").setConstant(0.28);
        } else {
            config.planCalcScore().getOrCreateModeParams("bike").setConstant(1.55);
            config.planCalcScore().getOrCreateModeParams("motorbike").setConstant(0.63);
        }

        // plain run to get plans files in first two iterations.
        new PatnaBaseCaseControler().runControler(config, patnaBestDecisionVariable);

        for(int plansFileIndex : plansFileIndexes) {
            for (int firstIt : firstIterations) {
                int lastIt = firstIt + 1000;
                String plansFile = tempOutDir + "/ITERS/it."+plansFileIndex+"/"+plansFileIndex+".plans.xml.gz";
                String outDir = baseDir+"/"+patnaBestDecisionVariable+"/"+plansFileIndex+"thPlans_"+firstIt+"thFirstIt_"+lastIt+"thLastIt/";
                if (! new File(outDir).exists() ) new File(outDir).mkdirs();

                config.plans().setInputFile(plansFile);
                config.controler().setOutputDirectory(outDir);
                config.controler().setFirstIteration(firstIt);
                config.controler().setLastIteration(lastIt);
                new PatnaBaseCaseControler().runControler(config,patnaBestDecisionVariable);
            }
        }
    }

    private void runControler(final Config config, final PatnaBestDecisionVariable patnaBestDecisionVariable) {

        // log statements
        LOGGER.info("====================================================");
        LOGGER.info("Strating patna base case with following properties: ");

        LOGGER.info("Inputs plans file is "+config.plans().getInputFile());
        LOGGER.info("The best overall decision variable is taken as "+patnaBestDecisionVariable.name());
        LOGGER.info("This means: ");
        if (patnaBestDecisionVariable.equals(PatnaBestDecisionVariable.BEST)) {
            LOGGER.info("used decision variable is the decision variable just before the reported best overall decision variable in opdyts.log file.");
        } else {
            LOGGER.info("used decision variable is the decision variable which is reported as best overall decision variable in opdyts.log file.");
        }

        LOGGER.info("Used ASCs for bike and motorbiek modes are "+config.planCalcScore().getOrCreateModeParams("bike").getConstant()+" and "+
                config.planCalcScore().getOrCreateModeParams("motorbike").getConstant()+" respectively.");

        LOGGER.info("The first and last iterations are "+ config.controler().getFirstIteration() + " and " + config.controler().getLastIteration() +" respectively.");
        LOGGER.info("====================================================");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        List<String> modes2consider = Arrays.asList("car","bike","motorbike");
        DistanceDistribution referenceStudyDistri = new PatnaNetworkModesOneBinDistanceDistribution(OPDYTS_SCENARIO_PATNA);
        OpdytsModalStatsControlerListener stasControlerListner = new OpdytsModalStatsControlerListener(modes2consider,referenceStudyDistri);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {

                addControlerListenerBinding().toInstance(stasControlerListner);

                this.bind(ModalShareEventHandler.class);
                this.addControlerListenerBinding().to(ModalShareControlerListener.class);

                this.bind(ModalTripTravelTimeHandler.class);
                this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
            }
        });

        controler.run();
    }
}
