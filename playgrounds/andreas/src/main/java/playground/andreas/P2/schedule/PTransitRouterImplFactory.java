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

package playground.andreas.P2.schedule;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
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
import playground.andreas.P2.stats.GexfPStat;
import playground.andreas.P2.stats.PCoopLogger;
import playground.andreas.P2.stats.PStats;

/**
 * Hook to register paratransit black box with MATSim
 * 
 * @author aneumann
 */
public class PTransitRouterImplFactory implements TransitRouterFactory, IterationStartsListener, StartupListener, ScoringListener{
	
	private final static Logger log = Logger.getLogger(PTransitRouterImplFactory.class);

	private TransitSchedule baseSchedule;
	private Vehicles baseVehicles;
	
	private TransitSchedule schedule;
	private Vehicles vehicles;
	
	private TransitRouterConfig config;
	private TransitRouterNetwork routerNetwork;
	
	private AgentsStuckHandlerImpl agentsStuckHandler;
	private PBox pBox;

	public PTransitRouterImplFactory(Controler controler) {
		PConfigGroup pConfig = (PConfigGroup) controler.getConfig().getModule(PConfigGroup.GROUP_NAME);
		this.pBox = new PBox(pConfig);
		if(pConfig.getReRouteAgentsStuck()){
			this.agentsStuckHandler = new AgentsStuckHandlerImpl();
		}
		controler.addControlerListener(new PStats(this.pBox, pConfig));
		controler.addControlerListener(new PCoopLogger(this.pBox, pConfig));
		controler.addControlerListener(new GexfPStat(pConfig));
	}

	@Override
	public TransitRouter createTransitRouter() {
		if(this.routerNetwork == null){
			this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		}		
		TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.config);
		return new TransitRouterImpl(this.config, this.routerNetwork, ttCalculator, ttCalculator);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.pBox.notifyStartup(event);
		this.routerNetwork = null;
		this.baseSchedule = event.getControler().getScenario().getTransitSchedule();
		this.baseVehicles = event.getControler().getScenario().getVehicles();
		this.schedule = addPTransitScheduleToOriginalOne(new PTransitSchedule(this.baseSchedule), this.pBox.getpTransitSchedule());
		((PScenarioImpl) event.getControler().getScenario()).setTransitSchedule(this.schedule);
		this.vehicles = this.addPVehiclesToOriginalOnes(this.baseVehicles, this.pBox.getVehicles());
		((PScenarioImpl) event.getControler().getScenario()).setVehicles(this.vehicles);
		this.config = new TransitRouterConfig(event.getControler().getScenario().getConfig().planCalcScore()
				, event.getControler().getScenario().getConfig().plansCalcRoute(), event.getControler().getScenario().getConfig().transitRouter(),
				event.getControler().getScenario().getConfig().vspExperimental());
		if(this.agentsStuckHandler != null){
			event.getControler().getEvents().addHandler(this.agentsStuckHandler);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		final Controler controler = event.getControler();
		if(event.getIteration() == 0){
			log.info("This is the first iteration. All lines were added by notifyStartup event.");
		} else {
			this.pBox.notifyIterationStarts(event);
			this.routerNetwork = null;
			this.schedule = addPTransitScheduleToOriginalOne(new PTransitSchedule(this.baseSchedule), this.pBox.getpTransitSchedule());
			((PScenarioImpl) event.getControler().getScenario()).setTransitSchedule(this.schedule);
			this.vehicles = this.addPVehiclesToOriginalOnes(this.baseVehicles, this.pBox.getVehicles());
			((PScenarioImpl) event.getControler().getScenario()).setVehicles(this.vehicles);
			
			if(this.agentsStuckHandler != null){
				ParallelPersonAlgorithmRunner.run(controler.getPopulation(), controler.getConfig().global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new PersonReRouteStuck(controler.createRoutingAlgorithm(), controler.getScenario(), agentsStuckHandler.getAgentsStuck());
					}
				});
			}
			this.dumpTransitScheduleAndVehicles(event);
		}
	}
	
	@Override
	public void notifyScoring(ScoringEvent event) {
		this.pBox.notifyScoring(event);	
	}

	private TransitSchedule addPTransitScheduleToOriginalOne(PTransitSchedule baseSchedule, TransitSchedule pSchedule) {
		
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
	
	private void dumpTransitScheduleAndVehicles(IterationStartsEvent event){
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(schedule);
		writer.write(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "transitSchedule.xml.gz"));
		
		VehicleWriterV1 writer2 = new VehicleWriterV1(event.getControler().getScenario().getVehicles());
		writer2.writeFile(event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "vehicles.xml.gz"));
	}

}