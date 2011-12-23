/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareScenarioControler.java
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

package playground.christoph.evacuation.controler;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;

import playground.christoph.energyflows.controller.EnergyFlowsController;
import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;
import playground.christoph.evacuation.mobsim.LegModeChecker;
import playground.christoph.evacuation.vehicles.AssignVehiclesToPlans;
import playground.christoph.evacuation.vehicles.CreateVehiclesForHouseholds;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;

/**
 * Prepares a scenario to be run as evacuation simulation.
 * 
 * <ul>
 * 	<li>Creates vehicles on household level based on model from bjaeggi (writes vehicles file and add vehicles to households).</li>
 * 	<li>Adapts plans to have consistent leg mode chains - it is ensured, that for each car leg a vehicles is available</li>
 * 	<li>Ensures that within a household with n vehicles only n persons have car legs in their plans</li>
 * </ul>
 * 
 * @author cdobler
 */
public class PrepareScenarioControler extends EnergyFlowsController implements StartupListener, SimulationInitializedListener {
	
	protected String[] householdVehicleFiles = new String[]{
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_AG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_AI.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_AR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_BE.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_BL.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_BS.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_FR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_GE.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_GL.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_GR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_JU.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_LU.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_NE.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_NW.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_OW.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SH.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SO.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SZ.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_TG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_TI.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_UR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_VD.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_VS.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_ZG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_ZH.txt"};

	protected LegModeChecker legModeChecker;
	protected CreateVehiclesForHouseholds createVehiclesForHouseholds;
	protected AssignVehiclesToPlans assignVehiclesToPlans;
	protected HouseholdVehicleAssignmentReader householdVehicleAssignmentReader;
	
	public PrepareScenarioControler(String[] args) {
		super(args);
		
		this.getQueueSimulationListener().add(this);
		this.addCoreControlerListener(this);
	}
	
	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		/*
		 * Using a LegModeChecker to ensure that all agents' plans have valid mode chains.
		 */
		legModeChecker = new LegModeChecker(this.createRoutingAlgorithm());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk, TransportMode.bike, TransportMode.pt});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenarioData.getPopulation());
		legModeChecker.printStatistics();

		/*
		 * Read household-vehicles-assignment files.
		 */
		this.householdVehicleAssignmentReader = new HouseholdVehicleAssignmentReader(this.scenarioData);
		for (String file : this.householdVehicleFiles) this.householdVehicleAssignmentReader.parseFile(file);
		this.householdVehicleAssignmentReader.createVehiclesForCrossboarderHouseholds();
		
		/*
		 * Create vehicles for households and add them to the scenario.
		 * When useVehicles is set to true, the scenario creates a Vehicles container if necessary.
		 */
		this.config.scenario().setUseVehicles(true);
		createVehiclesForHouseholds = new CreateVehiclesForHouseholds(this.scenarioData, this.householdVehicleAssignmentReader.getAssignedVehicles());
		createVehiclesForHouseholds.run();
		
		/*
		 * Assign vehicles to agent's plans.
		 */
		this.assignVehiclesToPlans = new AssignVehiclesToPlans(this.scenarioData, this.createRoutingAlgorithm());
		for (Household household : ((ScenarioImpl) scenarioData).getHouseholds().getHouseholds().values()) {
			this.assignVehiclesToPlans.run(household);
		}
		
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household.
		 */
		MobsimFactory mobsimFactory = new EvacuationQSimFactory();
		this.setMobsimFactory(mobsimFactory);
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		/*
		 * We replace the selected plan of each agent with the executed plan which
		 * is adapted by the within day replanning modules.
		 * So far, this is necessary because some modules, like e.g. EventsToScore,
		 * use person.getSelectedPlan(). However, when using within-day replanning
		 * the selected plan might be different than the executed plan which
		 * in turn will result in code that crashes...
		 */
		for (MobsimAgent agent : ((QSim)e.getQueueSimulation()).getAgents()) {
			if (agent instanceof ExperimentalBasicWithindayAgent) {
				Plan executedPlan = ((ExperimentalBasicWithindayAgent) agent).getSelectedPlan();
				PersonImpl person = (PersonImpl)((ExperimentalBasicWithindayAgent) agent).getPerson();
				person.removePlan(person.getSelectedPlan());
				person.addPlan(executedPlan);
				person.setSelectedPlan(executedPlan);
			}
		}
		
		/*
		 * Assign vehicles to agent's plans.
		 */
		this.assignVehiclesToPlans = new AssignVehiclesToPlans(this.scenarioData, this.createRoutingAlgorithm());
		for (Household household : ((ScenarioImpl) scenarioData).getHouseholds().getHouseholds().values()) {
			this.assignVehiclesToPlans.run(household);
		}
	}
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new PrepareScenarioControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
	
}
