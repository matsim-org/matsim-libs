/* *********************************************************************** *
 * project: org.matsim.*
 * SimCompare.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.locChoice;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.sim.analysis.ActivityDistanceTask;
import playground.johannes.socialnetworks.sim.analysis.LegDurationTask;
import playground.johannes.socialnetworks.sim.analysis.Trajectory;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryAnalyzer;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryEventsBuilder;
import playground.johannes.socialnetworks.sim.interaction.PseudoSim;

/**
 * @author illenberger
 *
 */
public class SimCompare {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile("/Users/jillenberger/Work/socialnets/locationChoice/simCompare/config.xml");
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.getParam("plans", "inputPlansFile"));
		
		TrajectoryAnalyzerTaskComposite task = new TrajectoryAnalyzerTaskComposite();
		task.addTask(new LegDurationTask());
		task.addTask(new ActivityDistanceTask(scenario.getNetwork(), 21781, new CartesianDistanceCalculator()));
		/*
		 * Run pseudo simulation
		 */
		EventsManager eventManager = EventsUtils.createEventsManager();
		
		TravelTime travelTime = new TravelTimeCalculator(scenario.getNetwork(), 900, 86400, new TravelTimeCalculatorConfigGroup());
		
		TrajectoryEventsBuilder trajectoryBuilder = new TrajectoryEventsBuilder(scenario.getPopulation());
		eventManager.addHandler(trajectoryBuilder);
		trajectoryBuilder.reset(0);
		
		PseudoSim pseudoSim = new PseudoSim();
		pseudoSim.run(scenario.getPopulation(), scenario.getNetwork(), travelTime, (EventsManagerImpl) eventManager);

		Set<Trajectory> trajectories = new HashSet<Trajectory>(trajectoryBuilder.getTrajectories().values());
		TrajectoryAnalyzer.analyze(trajectories, task, "/Users/jillenberger/Work/socialnets/locationChoice/simCompare/pseudoStats");
		/*
		 * Run queue simulation
		 */
		eventManager = EventsUtils.createEventsManager();
		trajectoryBuilder = new TrajectoryEventsBuilder(scenario.getPopulation());
		eventManager.addHandler(trajectoryBuilder);
		trajectoryBuilder.reset(0);
		
		QueueSimulationFactory qFactory = new QueueSimulationFactory();
		QueueSimulation queueSim = (QueueSimulation) qFactory.createMobsim(scenario, eventManager);
//		queueSim.setControlerIO(controlerIO);
//		new File(controlerIO.getIterationPath(it)).mkdirs();
//		queueSim.setIterationNumber(it);
		queueSim.run();
		
		trajectories = new HashSet<Trajectory>(trajectoryBuilder.getTrajectories().values());
		TrajectoryAnalyzer.analyze(trajectories, task, "/Users/jillenberger/Work/socialnets/locationChoice/simCompare/queueStats");
	}

}
