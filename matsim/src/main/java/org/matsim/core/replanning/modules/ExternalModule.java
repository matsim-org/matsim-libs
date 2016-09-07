/* *********************************************************************** *
 * project: org.matsim.*
 * ExternalModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.modules;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ExeRunner;

/**
 * Basic wrapper for any call to external "plans-to-plans" modules. As basic handling of
 * such modules is alike for every module:
 * 1.) Write a plans header
 * 2.) dump every person with the selected plan only
 * 3.) close plans file and write a config file based on a config template file
 * 4.) Execute the external program with this config file
 * 5.) Mutate original plans according to re-read plans
 *
 * @author dstrippgen
 * @author mrieser
 * @author michaz
 */
public class ExternalModule implements PlanStrategyModule {

    interface ExeRunnerDelegate {
        public boolean invoke();
    }

	private static final String SCENARIO = "scenario";
	private static final String SCENARIO_INPUT_PLANS_FILENAME = "inputPlansFilename";
	private static final String SCENARIO_WORKING_PLANS_FILENAME = "workingPlansFilename";
	private static final String SCENARIO_WORKING_EVENTS_TXT_FILENAME = "workingEventsTxtFilename";
	private static final String SCENARIO_NETWORK_FILENAME = "networkFilename";

	private static final String ExternalInFileName = "plans.in.xml";
	private static final String ExternalOutFileName = "plans.out.xml";
	private static final String ExternalConfigFileName = "config.xml";

	private final Scenario scenario;
    private final ExeRunnerDelegate exeRunnerDelegate;
    private String modulePrefix = "";
	private String outFileRoot = "";

	private final OutputDirectoryHierarchy controler;
	
	private int currentIteration = -1;
	
	private Population exportPopulation;
	
	private Map<Id<Person>, Plan> plansToMutate = new HashMap<>();

	public ExternalModule(final String exePath, final String moduleId, final OutputDirectoryHierarchy controler, final Scenario scenario) {
        this.modulePrefix = moduleId + "_";
		this.controler = controler;
		this.outFileRoot = controler.getTempPath() + "/";
		this.scenario = scenario;
        this.exeRunnerDelegate = new ExeRunnerDelegate() {
            @Override
            public boolean invoke() {
                String cmd = exePath + " " + outFileRoot + modulePrefix + ExternalConfigFileName;
                String logfilename = controler.getIterationFilename(currentIteration, modulePrefix + "stdout.log");
                return (ExeRunner.run(cmd, logfilename, 3600) == 0);
            }
        };
	}

    ExternalModule(ExeRunnerDelegate exeRunnerDelegate, final String moduleId, final OutputDirectoryHierarchy controler, final Scenario scenario) {
        this.modulePrefix = moduleId + "_";
        this.controler = controler;
        this.outFileRoot = controler.getTempPath() + "/";
        this.scenario = scenario;
        this.exeRunnerDelegate = exeRunnerDelegate;
    }

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		this.currentIteration = replanningContext.getIteration();
		this.exportPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
	}

	@Override
	public void handlePlan(final Plan plan) {
		// Creating a dummy population which only contains the plans which are passed here.
		// I need to copy the plans because I am not supposed to add a plan to a different Person.
		// I also need to memorize the plans which are passed here, because I am supposed to mutate them.
		
		final Person personWithOnlySelectedPlan = this.exportPopulation.getFactory().createPerson(plan.getPerson().getId());
		final Plan planForNewPerson = PopulationUtils.createPlan(personWithOnlySelectedPlan);
		PopulationUtils.copyFromTo(plan, planForNewPerson);
		personWithOnlySelectedPlan.addPlan(planForNewPerson);
		this.exportPopulation.addPerson(personWithOnlySelectedPlan);
		this.plansToMutate.put(plan.getPerson().getId(), plan);
	}

	@Override
	public void finishReplanning() {
		exportPopulation();
        prepareExternalExeConfig();
        boolean successful = this.exeRunnerDelegate.invoke();
        if (successful) {
			importPopulationAndMutatePlans();
		} else {
			throw new RuntimeException("External Replanning exited with error.");
		}
	}

    private void exportPopulation() {
		String filename = this.outFileRoot + this.modulePrefix + ExternalInFileName;
		PopulationWriter plansWriter = new PopulationWriter(exportPopulation, scenario.getNetwork());
		plansWriter.write(filename);
	}

    private void prepareExternalExeConfig() {
		Config extConfig;
		String configFileName = this.scenario.getConfig().strategy().getExternalExeConfigTemplate();
		if (configFileName == null) {
			extConfig = new Config();
		} else {
			extConfig = new Config();
			ConfigReader reader = new ConfigReader(extConfig);
			reader.readFile(configFileName);
		}
		// Change scenario config according to given output- and input-filenames: events, plans, network
		extConfig.setParam(SCENARIO, SCENARIO_INPUT_PLANS_FILENAME, this.outFileRoot + "/" + this.modulePrefix + ExternalInFileName);
		extConfig.setParam(SCENARIO, SCENARIO_WORKING_PLANS_FILENAME, this.outFileRoot + "/" + this.modulePrefix + ExternalOutFileName);
		extConfig.setParam(SCENARIO, SCENARIO_WORKING_EVENTS_TXT_FILENAME, this.controler.getIterationFilename(this.currentIteration - 1, "events.txt"));
		String networkFilename = this.scenario.getConfig().findParam("network", "inputNetworkFile");
		extConfig.setParam(SCENARIO, SCENARIO_NETWORK_FILENAME, networkFilename);
		new ConfigWriter(extConfig).write(this.outFileRoot + this.modulePrefix + ExternalConfigFileName);
	}

	private void importPopulationAndMutatePlans() {
		Scenario dummyScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader plansReader = new PopulationReader(dummyScenario);
		Population plans = dummyScenario.getPopulation();
		plansReader.readFile(this.outFileRoot + this.modulePrefix + ExternalOutFileName);
		new UpdatePlansAlgo().run(plans);
	}

	private class UpdatePlansAlgo extends AbstractPersonAlgorithm {
		@Override
		public void run(final Person dummyPerson) {
			Plan newPlan = dummyPerson.getPlans().get(0);
			Plan planToMutate = plansToMutate.get(dummyPerson.getId());
            planToMutate.getPlanElements().clear();
			PopulationUtils.copyFromTo(newPlan, planToMutate);
		}
	}

}
