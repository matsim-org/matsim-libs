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

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.vehicles.VehicleWriterV1;

import playground.christoph.controler.KTIEnergyFlowsController;
import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;
import playground.christoph.evacuation.mobsim.LegModeChecker;
import playground.christoph.evacuation.vehicles.AssignVehiclesToPlans;
import playground.christoph.evacuation.vehicles.CreateVehiclesForHouseholds;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;

import java.util.Arrays;

/**
 * Prepares a scenario to be run as evacuation simulation.
 * 
 * <ul>
 * 	<li>Creates vehicles on household level based on model from bjaeggi (writes vehicles file and add vehicles to households).</li>
 * 	<li>Adapts plans to have consistent leg mode chains - it is ensured, that for each car leg a vehicles is available.</li>
 * 	<li>Ensures that within a household with n vehicles only n persons have car legs in their plans.</li>
 * </ul>
 * 
 * @author cdobler
 */
public class PrepareScenarioControler extends KTIEnergyFlowsController implements StartupListener, IterationStartsListener, ReplanningListener {

	public static final String FILENAME_VEHICLES = "output_vehicles.xml.gz";
	
	protected String[] householdVehicleFiles = new String[] {
			"Fahrzeugtypen_Kanton_AG.txt", "Fahrzeugtypen_Kanton_AI.txt", "Fahrzeugtypen_Kanton_AR.txt",
			"Fahrzeugtypen_Kanton_BE.txt", "Fahrzeugtypen_Kanton_BL.txt", "Fahrzeugtypen_Kanton_BS.txt",
			"Fahrzeugtypen_Kanton_FR.txt", "Fahrzeugtypen_Kanton_GE.txt", "Fahrzeugtypen_Kanton_GL.txt",
			"Fahrzeugtypen_Kanton_GR.txt", "Fahrzeugtypen_Kanton_JU.txt", "Fahrzeugtypen_Kanton_LU.txt",
			"Fahrzeugtypen_Kanton_NE.txt", "Fahrzeugtypen_Kanton_NW.txt", "Fahrzeugtypen_Kanton_OW.txt",
			"Fahrzeugtypen_Kanton_SG.txt", "Fahrzeugtypen_Kanton_SH.txt", "Fahrzeugtypen_Kanton_SO.txt",
			"Fahrzeugtypen_Kanton_SZ.txt", "Fahrzeugtypen_Kanton_TG.txt", "Fahrzeugtypen_Kanton_TI.txt",
			"Fahrzeugtypen_Kanton_UR.txt", "Fahrzeugtypen_Kanton_VD.txt", "Fahrzeugtypen_Kanton_VS.txt",
			"Fahrzeugtypen_Kanton_ZG.txt", "Fahrzeugtypen_Kanton_ZH.txt"};
	
	protected LegModeChecker legModeChecker;
	protected CreateVehiclesForHouseholds createVehiclesForHouseholds;
	protected AssignVehiclesToPlans assignVehiclesToPlans;
	protected HouseholdVehicleAssignmentReader householdVehicleAssignmentReader;
	
	public PrepareScenarioControler(String[] args) {
		// don't hand path to vehicles files over to super class
		super(Arrays.copyOfRange(args, 0, 2));
		
		this.addCoreControlerListener(this);
		
		String vehiclesFilesPath = args[2];
		if (!vehiclesFilesPath.endsWith("/")) vehiclesFilesPath = vehiclesFilesPath + "/";
		for (int i = 0; i < householdVehicleFiles.length; i++) householdVehicleFiles[i] = vehiclesFilesPath + householdVehicleFiles[i];
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}
	
	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		
		/*
		 * Remove all un-selected plans from agents' memories
		 */
		for (Person person : this.getScenario().getPopulation().getPersons().values()) {
			((PersonImpl) person).removeUnselectedPlans();
		}
		
		/*
		 * Using a LegModeChecker to ensure that all agents' plans have valid mode chains.
		 */
		TravelDisutility travelDisutility = this.getTravelDisutilityFactory().createTravelDisutility(this.getLinkTravelTimes(), this.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.getLinkTravelTimes());
		TripRouterFactory tripRouterFactory = new TripRouterFactoryBuilderWithDefaults().build(this.getScenario());

        legModeChecker = new LegModeChecker(tripRouterFactory.instantiateAndConfigureTripRouter(routingContext), getScenario().getNetwork());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk, TransportMode.bike, TransportMode.pt});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.getScenario().getPopulation());
		legModeChecker.printStatistics();

		/*
		 * Read household-vehicles-assignment files.
		 */
		this.householdVehicleAssignmentReader = new HouseholdVehicleAssignmentReader(this.getScenario());
		for (String file : this.householdVehicleFiles) this.householdVehicleAssignmentReader.parseFile(file);
		this.householdVehicleAssignmentReader.createVehiclesForCrossboarderHouseholds();
		
		/*
		 * Create vehicles for households and add them to the scenario.
		 * When useVehicles is set to true, the scenario creates a Vehicles container if necessary.
		 */
		this.getConfig().scenario().setUseVehicles(true);
		createVehiclesForHouseholds = new CreateVehiclesForHouseholds(this.getScenario(), this.householdVehicleAssignmentReader.getAssignedVehicles());
		createVehiclesForHouseholds.run();
		
		/*
		 * Write vehicles to file.
		 */
		Logger.getLogger(this.getClass()).fatal("cannot say if the following should be vehicles or transit vehicles; aborting ... .  kai, feb'15");
		System.exit(-1); 
		new VehicleWriterV1(getScenario().getTransitVehicles()).writeFile(this.getControlerIO().getOutputFilename(FILENAME_VEHICLES));
		
		/*
		 * Assign vehicles to agent's plans.
		 */
		this.assignVehiclesToPlans = new AssignVehiclesToPlans(this.getScenario(), event.getControler().getTripRouterProvider().get());
		for (Household household : ((ScenarioImpl) getScenario()).getHouseholds().getHouseholds().values()) {
			this.assignVehiclesToPlans.run(household);
		}
		this.assignVehiclesToPlans.printStatistics();
		
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household.
		 */
		final MobsimFactory mobsimFactory = new EvacuationQSimFactory(null, null, null, null);
		this.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return mobsimFactory.createMobsim(getScenario(), getEvents());
					}
				});
			}
		});
	}
	
	/*
	 * PersonPrepareForSim is run before the first iteration is started.
	 * There, some routes might be recalculated and their vehicleIds set to null.
	 * As a result, we have to reassign the vehicles to the agents.
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() == this.getConfig().controler().getFirstIteration()) {
			this.assignVehiclesToPlans.reassignVehicles();
		}
	}
	
	/*
	 * So far, vehicle Ids are deleted when a person's route is updated during
	 * the replanning. Therefore we have to set the vehicles again after the
	 * replanning.
	 */
	@Override
	public void notifyReplanning(ReplanningEvent event) {
		this.assignVehiclesToPlans.reassignVehicles();
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
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
			controler.run();
		}
		System.exit(0);
	}
	
}
