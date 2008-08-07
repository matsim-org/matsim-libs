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

package org.matsim.replanning.modules;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.config.MatsimConfigReader;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.population.PopulationWriterHandler;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonCalcTimes;
import org.matsim.utils.misc.ExeRunner;

/**
 * Basic wrapper for any call to external "planstoplans" modules. As basic handling of
 * such modules is alike for every module:
 * 1.) Write a plans header
 * 2.) dump every person with the selected plan only
 * 3.) close plans file and write a config file based on a config template file
 * 4.) Exe-cute the external program with this config file
 * 5.) Re-read plans and exchange selected plan by a new one or append new plan
 *
 * @author dstrippgen
 * @author mrieser
 */
public class ExternalModule implements StrategyModuleI {

	protected static final String ExternalInFileName = "plans.in.xml";
	protected static final String ExternalOutFileName = "plans.out.xml";
	protected static final String ExternalConfigFileName = "config.xml";

	/** holds a personId and the reference to the person for reloading the plans later */
	private final TreeMap<Id, Person> persons = new TreeMap<Id, Person>();
	protected PopulationWriter plansWriter = null;
	private PopulationWriterHandler handler = null;
	private BufferedWriter writer = null;
	protected Config extConfig;
	protected String exePath = "";
	protected String moduleId = "";
	protected String outFileRoot = "";

	public ExternalModule(final String exePath, final String moduleId) {
		this.exePath = exePath;
		this.moduleId = moduleId + "_";
		this.outFileRoot = Controler.getTempPath();
	}

	public void init() {
		this.persons.clear();
		this.plansWriter = getPlansWriterHandler();
		this.plansWriter.writeStartPlans();
		this.handler = this.plansWriter.getHandler();
		this.writer = this.plansWriter.getWriter();
	}

	protected PopulationWriter getPlansWriterHandler() {
		String filename = this.outFileRoot + "/" + this.moduleId + ExternalInFileName;
		String version = "v4";
		return new PopulationWriter(new Population(Population.USE_STREAMING), filename, version);
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
			for (int jj = 0; jj < plan.getActsLegs().size(); jj++) {
				if (jj % 2 == 0) {
					Act act = (Act)plan.getActsLegs().get(jj);
					this.handler.startAct(act, this.writer);
					this.handler.endAct(this.writer);
				} else {
					Leg leg = (Leg)plan.getActsLegs().get(jj);
					this.handler.startLeg(leg, this.writer);
					// route
					if (leg.getRoute() != null) {
						Route r = leg.getRoute();
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

	public void finish() {
		this.plansWriter.writeEndPlans();
		if (this.persons.size() == 0) {
			return;
		}
		if (callExe()) {
			// the exe returned with error = 0, == no error
			// Read back  plans file and change plans for persons in file
			reReadPlans();
		} else {
			Logger.getLogger(this.getClass()).warn("External exe returned with an error. Plans were NOT altered!");
		}

	}

	public void prepareExternalExeConfig() {
		String configFileName = Gbl.getConfig().strategy().getExternalExeConfigTemplate();
		if (configFileName == null) {
			this.extConfig = new Config();
		} else {
			this.extConfig = new Config();
			MatsimConfigReader reader = new MatsimConfigReader(this.extConfig);
			reader.readFile(configFileName);
		}
	}

	public void writeExternalExeConfig() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(this.outFileRoot + "/" + this.moduleId + ExternalConfigFileName));
			ConfigWriter configWriter = new ConfigWriter(this.extConfig, writer);
			configWriter.write();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private boolean callExe() {
		prepareExternalExeConfig();
		writeExternalExeConfig();

		String cmd = this.exePath + " " + this.outFileRoot + "/" + this.moduleId + ExternalConfigFileName;
		String logfilename = Controler.getIterationFilename(this.moduleId + "stdout.log");

		return (ExeRunner.run(cmd, logfilename, 3600) == 0);
	}

	private void reReadPlans() {
		Population plans = new Population(Population.NO_STREAMING);
		PopulationReader plansReader = getPlansReader(plans);
		plans.addAlgorithm(new PersonCalcTimes());
		plans.addAlgorithm(new UpdatePlansAlgo(this.persons));
		plansReader.readFile(this.outFileRoot + "/" + this.moduleId + ExternalOutFileName);
		plans.printPlansCount();
		plans.runAlgorithms();
	}

	protected PopulationReader getPlansReader(final Population plans) {
		PopulationReader plansReader = new MatsimPlansReader(plans);
		return plansReader;
	}

	private static class UpdatePlansAlgo extends PersonAlgorithm {

		private final TreeMap<Id, Person> persons;

		public UpdatePlansAlgo(final TreeMap<Id, Person> persons) {
			this.persons = persons;
		}

		@Override
		public void run(final Person dummyperson) {
			Plan newplan = dummyperson.getPlans().get(0);
			// Find the original person
			Id Id = dummyperson.getId();
			Person person = this.persons.get(Id);

			// replace / append the new plan
			person.exchangeSelectedPlan(newplan, false);
		}
	}

}
