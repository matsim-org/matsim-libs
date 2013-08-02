/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P2.hook;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.P2.schedule.PTransitScheduleImpl;
import playground.andreas.P2.stats.StatsManager;
import playground.andreas.P2.stats.abtractPAnalysisModules.lineSetter.PtMode2LineSetter;

/**
 * Hook to register paratransit black box with MATSim
 * 
 * @author aneumann
 */
public class PHook implements IterationStartsListener, StartupListener, ScoringListener{
	
	private final static Logger log = Logger.getLogger(PHook.class);

	private TransitSchedule baseSchedule;
	private Vehicles baseVehicles;
	
	private TransitSchedule schedule;
	private Vehicles vehicles;
	
	private PTransitRouterFactory pTransitRouterFactory = null;
	private PVehiclesFactory pVehiclesFactory = null;
	
	private AgentsStuckHandlerImpl agentsStuckHandler;
	private PBox pBox;

	private StatsManager statsManager;

	private PersonReRouteStuckFactory stuckFactory;

	public PHook(Controler controler) {
		this(controler, null, null, null, null);
	}
	
	public PHook(Controler controler, PtMode2LineSetter lineSetter, PTransitRouterFactory pTransitRouterFactory, PersonReRouteStuckFactory stuckFactory, Class<? extends TripRouterFactory> tripRouterFactory){
		PConfigGroup pConfig = (PConfigGroup) controler.getConfig().getModule(PConfigGroup.GROUP_NAME);
		this.pBox = new PBox(pConfig);
		this.pTransitRouterFactory = pTransitRouterFactory;
		if (this.pTransitRouterFactory == null) {
			this.pTransitRouterFactory = new PTransitRouterFactory(pConfig.getPtEnabler());
		}
		// When setting a TransitRouterFactory and also a TripRouterFactory in the controler a RuntimeException is thrown.
//		controler.setTransitRouterFactory(this.pTransitRouterFactory);
		controler.setMobsimFactory(new PQSimFactory());
		this.pVehiclesFactory = new PVehiclesFactory(pConfig);

		if(pConfig.getReRouteAgentsStuck()){
			this.agentsStuckHandler = new AgentsStuckHandlerImpl();
			if(stuckFactory == null){
				this.stuckFactory = new PersonReRouteStuckFactoryImpl();
			}else{
				this.stuckFactory = stuckFactory;
			}
		}
		
		controler.setTripRouterFactory(PTripRouterFactoryFactory.getTripRouterFactoryInstance(controler, tripRouterFactory, pTransitRouterFactory));
		this.statsManager = new StatsManager(controler, pConfig, this.pBox, lineSetter); 
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.statsManager.notifyStartup(event);
		this.pBox.notifyStartup(event);
		this.baseSchedule = event.getControler().getScenario().getTransitSchedule();
		this.baseVehicles = ((ScenarioImpl) event.getControler().getScenario()).getVehicles();
		this.schedule = addPTransitScheduleToOriginalOne(this.baseSchedule, this.pBox.getpTransitSchedule());
		((PScenarioImpl) event.getControler().getScenario()).setTransitSchedule(this.schedule);
		this.vehicles = this.addPVehiclesToOriginalOnes(this.baseVehicles, this.pVehiclesFactory.createVehicles(this.pBox.getpTransitSchedule()));
		((PScenarioImpl) event.getControler().getScenario()).setVehicles(this.vehicles);
		
		this.pTransitRouterFactory.createTransitRouterConfig(event.getControler().getConfig());
		this.pTransitRouterFactory.updateTransitSchedule(this.schedule);
		
		if(this.agentsStuckHandler != null){
			event.getControler().getEvents().addHandler(this.agentsStuckHandler);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		final Controler controler = event.getControler();
		if(event.getIteration() == controler.getFirstIteration()){
			log.info("This is the first iteration. All lines were added by notifyStartup event.");
		} else {
			this.pBox.notifyIterationStarts(event);
			this.schedule = addPTransitScheduleToOriginalOne(this.baseSchedule, this.pBox.getpTransitSchedule());
			((PScenarioImpl) event.getControler().getScenario()).setTransitSchedule(this.schedule);
			this.vehicles = this.addPVehiclesToOriginalOnes(this.baseVehicles, this.pVehiclesFactory.createVehicles(this.pBox.getpTransitSchedule()));
			((PScenarioImpl) event.getControler().getScenario()).setVehicles(this.vehicles);
			
			this.pTransitRouterFactory.updateTransitSchedule(this.schedule);
			
			if(this.agentsStuckHandler != null){
				ParallelPersonAlgorithmRunner.run(controler.getPopulation(), controler.getConfig().global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return stuckFactory.getReRouteStuck(controler.createRoutingAlgorithm(), ((ScenarioImpl)controler.getScenario()), agentsStuckHandler.getAgentsStuck());
					}
				});
			}
		}
		this.dumpTransitScheduleAndVehicles(event.getControler(), event.getIteration());
	}
	
	@Override
	public void notifyScoring(ScoringEvent event) {
		this.pBox.notifyScoring(event);	
	}

	private TransitSchedule addPTransitScheduleToOriginalOne(TransitSchedule baseSchedule, TransitSchedule pSchedule) {
		TransitSchedule schedule = new PTransitScheduleImpl(baseSchedule.getFactory());
		
		if(pSchedule == null){
			log.info("pSchedule does not exist, returning non modified one");
			return baseSchedule;
		}
		
		for (TransitStopFacility pStop : baseSchedule.getFacilities().values()) {
			schedule.addStopFacility(pStop);
		}
		for (TransitStopFacility pStop : pSchedule.getFacilities().values()) {
			schedule.addStopFacility(pStop);
		}
		
		for (TransitLine pLine : baseSchedule.getTransitLines().values()) {
			schedule.addTransitLine(pLine);
		}
		for (TransitLine pLine : pSchedule.getTransitLines().values()) {
			schedule.addTransitLine(pLine);
		}
		
		return schedule;
	}
	
	private Vehicles addPVehiclesToOriginalOnes(Vehicles baseVehicles, Vehicles pVehicles){
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		
		vehicles.getVehicleTypes().putAll(baseVehicles.getVehicleTypes());
		vehicles.getVehicles().putAll(baseVehicles.getVehicles());
		
		vehicles.getVehicleTypes().putAll(pVehicles.getVehicleTypes());
		vehicles.getVehicles().putAll(pVehicles.getVehicles());
		
		return vehicles;
	}
	
	private void dumpTransitScheduleAndVehicles(Controler controler, int iteration){
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(this.schedule);
		VehicleWriterV1 writer2 = new VehicleWriterV1(((ScenarioImpl) controler.getScenario()).getVehicles());
		writer.write(controler.getControlerIO().getIterationFilename(iteration, "transitSchedule.xml.gz"));
		writer2.writeFile(controler.getControlerIO().getIterationFilename(iteration, "vehicles.xml.gz"));
	}
}