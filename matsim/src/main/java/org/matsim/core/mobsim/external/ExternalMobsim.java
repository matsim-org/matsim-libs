/* *********************************************************************** *
 * project: org.matsim.*
 * ExternalMobsim.java
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

package org.matsim.core.mobsim.external;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.AbstractPopulationWriterHandler;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.PopulationWriterHandlerImplV4;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ExeRunner;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ExternalMobsim implements Mobsim {

	private static final String CONFIG_MODULE = "simulation";

	protected final Scenario scenario;
	protected final EventsManager events;

	protected String plansFileName = null;
	protected String eventsFileName = null;
	protected String configFileName = null;

	protected String executable = null;

	private static final Logger log = Logger.getLogger(ExternalMobsim.class);

	private Integer iterationNumber = null;
	protected OutputDirectoryHierarchy controlerIO;


	public ExternalMobsim(final Scenario scenario, final EventsManager events) {
		this.scenario = scenario;
		this.events = events;
		init();
	}

	protected void init() {
		this.plansFileName = "ext_plans.xml";
		this.eventsFileName = "ext_events.txt";
		this.configFileName = "ext_config.xml";

		this.executable = this.scenario.getConfig().getParam(CONFIG_MODULE, "externalExe");
	}

	@Override
	public void run() {
		String iterationPlansFile = this.controlerIO.getIterationFilename(this.iterationNumber, this.plansFileName);
		String iterationEventsFile = this.controlerIO.getIterationFilename(this.iterationNumber, this.eventsFileName);
		String iterationConfigFile = this.controlerIO.getIterationFilename(this.iterationNumber, configFileName);

		try {
			writePlans(iterationPlansFile);
			writeConfig(iterationPlansFile, iterationEventsFile, iterationConfigFile);
			runExe(iterationConfigFile);
			readEvents(iterationEventsFile);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


	protected void writeConfig(final String iterationPlansFile, final String iterationEventsFile, final String iterationConfigFile) throws FileNotFoundException, IOException {
		log.info("writing config for external mobsim");
		Config simConfig = this.scenario.getConfig();
		Config extConfig = new Config();
		// network
		ConfigGroup module = extConfig.createModule("network");
		module.addParam("inputNetworkFile", simConfig.network().getInputFile());
		module.addParam("localInputDTD", "dtd/matsim_v1.dtd");
		// plans
		module = extConfig.createModule("plans");
		module.addParam("inputPlansFile", iterationPlansFile);
		module.addParam("inputVersion", "matsimXMLv4");
		// events
		module = extConfig.createModule("events");
		module.addParam("outputFile", iterationEventsFile);
		module.addParam("outputFormat", "matsimTXTv1");
		// deqsim
		module = extConfig.createModule(CONFIG_MODULE);
		module.addParam("startTime", simConfig.getParam(CONFIG_MODULE, "startTime"));
		module.addParam("endTime", simConfig.getParam(CONFIG_MODULE, "endTime"));

		new ConfigWriter(extConfig).write(iterationConfigFile);
	}

	protected void writePlans(final String iterationPlansFile) throws FileNotFoundException, IOException {
		log.info("writing plans for external mobsim");
		PopulationImpl pop = (PopulationImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		pop.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(pop, this.scenario.getNetwork());
		AbstractPopulationWriterHandler handler = new PopulationWriterHandlerImplV4(this.scenario.getNetwork());
		plansWriter.setWriterHandler(handler);
		plansWriter.writeStartPlans(iterationPlansFile);
		BufferedWriter writer = plansWriter.getWriter();
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			if (plan != null) {
				/* we have to re-implement a custom writer here, because we only want to
				 * write a single plan (the selected one) and not all plans of the person.
				 */
				handler.startPerson(person, writer);
				handler.startPlan(plan, writer);

				// act/leg
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						handler.startAct(act, writer);
						handler.endAct(writer);
					} else if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						handler.startLeg(leg, writer);
						// route
						if (leg.getRoute() != null) {
							NetworkRoute r = (NetworkRoute) leg.getRoute();
							handler.startRoute(r, writer);
							handler.endRoute(writer);
						}
						handler.endLeg(writer);
					}
				}
				handler.endPlan(writer);
				handler.endPerson(writer);
				handler.writeSeparator(writer);
				writer.flush();
			}
		}
		handler.endPlans(writer);
		writer.flush();
		writer.close();
	}

	@SuppressWarnings("unused") /* do now show warnings that this method does not throw any exceptions,
	as classes inheriting from this class may throw exceptions in their implementation of this method. */
	protected void runExe(final String iterationConfigFile) throws FileNotFoundException, IOException {
		String cmd = this.executable + " " + iterationConfigFile;
		log.info("running command: " + cmd);
		Gbl.printMemoryUsage();
		String logfileName = this.controlerIO.getIterationFilename(this.getIterationNumber(), "mobsim.log");
		int timeout = ((SimulationConfigGroup) this.scenario.getConfig().getModule(SimulationConfigGroup.GROUP_NAME)).getExternalTimeOut();
		int exitcode = ExeRunner.run(cmd, logfileName, timeout);
		if (exitcode != 0) {
			throw new RuntimeException("There was a problem running the external mobsim. exit code: " + exitcode);
		}
	}

	@SuppressWarnings("unused") /* do now show warnings that this method does not throw any exceptions,
	as classes inheriting from this class may throw exceptions in their implementation of this method. */
	protected void readEvents(final String iterationEventsFile) throws FileNotFoundException, IOException {
		log.info("reading events from external mobsim");
		new MatsimEventsReader(this.events).readFile(iterationEventsFile);
	}

	public Integer getIterationNumber() {
		return iterationNumber;
	}

	public void setIterationNumber(Integer iterationNumber) {
		this.iterationNumber = iterationNumber;
	}

	public void setControlerIO(OutputDirectoryHierarchy controlerIO) {
		this.controlerIO = controlerIO;
	}


}
