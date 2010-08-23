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

package playground.mzilske.city2000w;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.PopulationWriterHandler;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.ExeRunner;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PersonCalcTimes;

/**
 * Basic wrapper for any call to external "plans-to-plans" modules. As basic handling of
 * such modules is alike for every module:
 * 1.) Write a plans header
 * 2.) dump every person with the selected plan only
 * 3.) close plans file and write a config file based on a config template file
 * 4.) Execute the external program with this config file
 * 5.) Re-read plans and exchange selected plan by a new one or append new plan
 *
 * @author dstrippgen
 * @author mrieser
 */
public class ExternalModule implements PlanStrategyModule {

	private static final String SCENARIO = "scenario";
	private static final String SCENARIO_INPUT_PLANS_FILENAME = "inputPlansFilename";
	private static final String SCENARIO_WORKING_PLANS_FILENAME = "workingPlansFilename";
	private static final String SCENARIO_WORKING_EVENTS_TXT_FILENAME = "workingEventsTxtFilename";
	private static final String SCENARIO_NETWORK_FILENAME = "networkFilename";

	protected static final String ExternalInFileName = "plans.in.xml";
	protected static final String ExternalOutFileName = "plans.out.xml";
	protected static final String ExternalConfigFileName = "config.xml";

	/** holds a personId and the reference to the person for reloading the plans later */
	private final TreeMap<Id, Person> persons = new TreeMap<Id, Person>();
	protected PopulationWriter plansWriter = null;
	private PopulationWriterHandler handler = null;
	private BufferedWriter writer = null;
	private final Scenario scenario;
	protected Config extConfig;
	protected String exePath = "";
	protected String moduleId = "";
	protected String outFileRoot = "";

	private Integer iterationNumber = null;
	private ControlerIO controlerIO;
	private int startIterationNumber;


	public ExternalModule(final String exePath, final String moduleId, final ControlerIO controlerIO, final Scenario scenario, int startIterationNumber) {
		this.exePath = exePath;
		this.moduleId = moduleId + "_";
		this.controlerIO = controlerIO;
		this.outFileRoot = controlerIO.getTempPath() + "/";
		this.scenario = scenario;
		this.startIterationNumber = startIterationNumber;
	}

	public void prepareReplanning() {
		String filename = this.outFileRoot + this.moduleId + ExternalInFileName;
		PopulationImpl pop = (PopulationImpl) new ScenarioImpl().getPopulation();
		pop.setIsStreaming(true);
		this.plansWriter = new PopulationWriter(pop, scenario.getNetwork());

		this.persons.clear();
		this.plansWriter.writeStartPlans(filename);
		this.handler = this.plansWriter.getHandler();
		this.writer = this.plansWriter.getWriter();
	}

	public void handlePlan(final Plan plan) {
		Person person = plan.getPerson();
		this.persons.put(person.getId(), person);

		try {
			/* we have to re-implement a custom writer here, because we only want to
			 * write a single plan (the selected one) and not all plans of the person.
			 */
			this.handler.startPerson(person, this.writer);
			this.handler.startPlan(plan, this.writer);

			// act/leg
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					this.handler.startAct(act, this.writer);
					this.handler.endAct(this.writer);
				} else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					this.handler.startLeg(leg, this.writer);
					// route
					if (leg.getRoute() != null) {
						NetworkRoute r = (NetworkRoute) leg.getRoute();
						this.handler.startRoute(r, this.writer);
						this.handler.endRoute(this.writer);
					}
					this.handler.endLeg(this.writer);
				}
			}
			this.handler.endPlan(this.writer);
			this.handler.endPerson(this.writer);
			this.handler.writeSeparator(this.writer);
			this.writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void finishReplanning() {
		this.plansWriter.writeEndPlans();
		if (this.persons.size() == 0) {
			return;
		}
		if (callExe()) {
			// the exe returned with error = 0, == no error
			// Read back  plans file and change plans for persons in file
			reReadPlans();
		} else {
			throw new RuntimeException("External Replanning failed! ");
		}
		startIterationNumber++;
	}

	private void prepareExternalExeConfig() {
		String configFileName = this.scenario.getConfig().strategy().getExternalExeConfigTemplate();
		if (configFileName == null) {
			this.extConfig = new Config();
		} else {
			this.extConfig = new Config();
			MatsimConfigReader reader = new MatsimConfigReader(this.extConfig);
			reader.readFile(configFileName);
		}

		// Change scenario config according to given output- and input-filenames: events, plans, network
		this.extConfig.setParam(SCENARIO, SCENARIO_INPUT_PLANS_FILENAME, this.outFileRoot + "/" + this.moduleId + ExternalInFileName);
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_PLANS_FILENAME, this.outFileRoot + "/" + this.moduleId + ExternalOutFileName);
		this.extConfig.setParam(SCENARIO, SCENARIO_WORKING_EVENTS_TXT_FILENAME, controlerIO.getIterationFilename(startIterationNumber - 1, "events.txt"));
		String networkFilename = this.scenario.getConfig().findParam("network", "inputNetworkFile");
		this.extConfig.setParam(SCENARIO, SCENARIO_NETWORK_FILENAME, networkFilename);
	}

	private void writeExternalExeConfig() {
		new ConfigWriter(this.extConfig).write(this.outFileRoot + this.moduleId + ExternalConfigFileName);
	}

	private boolean callExe() {
		prepareExternalExeConfig();
		writeExternalExeConfig();

		String cmd = this.exePath + " " + this.outFileRoot + this.moduleId + ExternalConfigFileName;
		String logfilename = controlerIO.getIterationFilename(this.iterationNumber, this.moduleId + "stdout.log");

		return (ExeRunner.run(cmd, logfilename, 3600) == 0);
	}

	private void reReadPlans() {
		Population plans = new ScenarioImpl().getPopulation();
		PopulationReader plansReader = getPlansReader(plans);
		plansReader.readFile(this.outFileRoot + this.moduleId + ExternalOutFileName);
		new PersonCalcTimes().run(plans);
		new UpdatePlansAlgo(this.persons).run(plans);
	}

	private PopulationReader getPlansReader(final Population plans) {
		PopulationReader plansReader = new MatsimPopulationReader(new PseudoScenario(this.scenario, plans));
		return plansReader;
	}

	private static class UpdatePlansAlgo extends AbstractPersonAlgorithm {

		private final TreeMap<Id, Person> persons;

		protected UpdatePlansAlgo(final TreeMap<Id, Person> persons) {
			this.persons = persons;
		}

		@Override
		public void run(final Person dummyperson) {
			Plan newplan = dummyperson.getPlans().get(0);
			// Find the original person
			Id Id = dummyperson.getId();
			Person person = this.persons.get(Id);

			// replace / append the new plan
			((PersonImpl) person).exchangeSelectedPlan(newplan, false);
		}
	}

	public Integer getIterationNumber() {
		return iterationNumber;
	}

	public void setIterationNumber(Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

	/**
	 * Provides a real scenario, but exchanges the population.
	 * Still, network and facilities can be reused that way.
	 *
	 * @author mrieser
	 */
	private static class PseudoScenario implements Scenario {

		private final Scenario scenario;
		private Population myPopulation;

		public PseudoScenario(final Scenario scenario, final Population population) {
			this.scenario = scenario;
			this.myPopulation = population;
		}

		@Override
		public Population getPopulation() {
			return this.myPopulation;
		}

		@Override
		public Coord createCoord(double x, double y) {
			return this.scenario.createCoord(x, y);
		}

		@Override
		public Id createId(String string) {
			return this.scenario.createId(string);
		}

		@Override
		public Config getConfig() {
			return this.scenario.getConfig();
		}

		@Override
		public Network getNetwork() {
			return this.scenario.getNetwork();
		}

		@Override
		public void addScenarioElement(Object o) {
			this.scenario.addScenarioElement(o);
		}

		@Override
		public <T> T getScenarioElement(Class<? extends T> klass) {
			return this.scenario.getScenarioElement(klass);
		}

		@Override
		public boolean removeScenarioElement(Object o) {
			return this.scenario.removeScenarioElement(o);
		}

	}

}
