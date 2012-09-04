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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

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

import playground.andreas.P2.ana.ActivityLocationsParatransitUser;
import playground.andreas.P2.ana.PAnalysisManager;
import playground.andreas.P2.ana.helper.PtMode2LineSetter;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.P2.stats.GexfPStat;
import playground.andreas.P2.stats.GexfPStatLight;
import playground.andreas.P2.stats.Line2GexfPStat;
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
	
	private TransitRouterConfig config = null;
	private boolean needToUpdateRouter = true;
	private TransitRouterNetwork routerNetwork = null;
	private TransitRouterFactory routerFactory = null;
	private String ptEnabler = null;
	
	private PVehiclesFactory pVehiclesFactory = null;
	
	private AgentsStuckHandlerImpl agentsStuckHandler;
	private PBox pBox;

	public PTransitRouterImplFactory(Controler controler) {
		this(controler, null);
	}
	
	public PTransitRouterImplFactory(Controler controler, PtMode2LineSetter lineSetter){
		PConfigGroup pConfig = (PConfigGroup) controler.getConfig().getModule(PConfigGroup.GROUP_NAME);
		this.pBox = new PBox(pConfig);
		this.ptEnabler = pConfig.getPtEnabler();
		this.pVehiclesFactory = new PVehiclesFactory(pConfig);
		if(pConfig.getReRouteAgentsStuck()){
			this.agentsStuckHandler = new AgentsStuckHandlerImpl();
		}
		controler.addControlerListener(new PStats(this.pBox, pConfig));
		controler.addControlerListener(new PCoopLogger(this.pBox, pConfig));
		controler.addControlerListener(new GexfPStat(pConfig, false));
//		controler.addControlerListener(new GexfPStat(pConfig, true));
		controler.addControlerListener(new GexfPStatLight(pConfig));
		controler.addControlerListener(new Line2GexfPStat(pConfig));
		if(lineSetter == null){
			controler.addControlerListener(new PAnalysisManager(pConfig, "pt_"));
		}else{
			controler.addControlerListener(new PAnalysisManager(pConfig, "pt_", lineSetter));
		}
		controler.addControlerListener(new ActivityLocationsParatransitUser(pConfig, 100.0));
	}

	@Override
	public TransitRouter createTransitRouter() {
		if(needToUpdateRouter) {
			// okay update all routers
			this.routerFactory = createSpeedyRouter();
			if(this.routerFactory == null) {
				log.warn("Could not create speedy router, fall back to normal one.");
				this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
			}
			needToUpdateRouter = false;
		}
		
		if (this.routerFactory == null) {
			// no speedy router available - return old one
			TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.config);
			return new TransitRouterImpl(this.config, routerNetwork, ttCalculator, ttCalculator);
		} else {
			return this.routerFactory.createTransitRouter();
		}
	}
	
	private TransitRouterFactory createSpeedyRouter() {
		try {
			Class<?> cls = Class.forName("com.senozon.matsim.pt.speedyrouter.SpeedyTransitRouterFactory");
			Constructor<?> ct = cls.getConstructor(new Class[] {TransitSchedule.class, TransitRouterConfig.class, String.class});
			return (TransitRouterFactory) ct.newInstance(new Object[] {this.schedule, this.config, this.ptEnabler});
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}


	@Override
	public void notifyStartup(StartupEvent event) {
		this.pBox.notifyStartup(event);
		this.needToUpdateRouter = true;
		this.baseSchedule = event.getControler().getScenario().getTransitSchedule();
		this.baseVehicles = event.getControler().getScenario().getVehicles();
		this.schedule = addPTransitScheduleToOriginalOne(this.baseSchedule, this.pBox.getpTransitSchedule());
		((PScenarioImpl) event.getControler().getScenario()).setTransitSchedule(this.schedule);
		this.vehicles = this.addPVehiclesToOriginalOnes(this.baseVehicles, this.pVehiclesFactory.createVehicles(this.pBox.getpTransitSchedule()));
		((PScenarioImpl) event.getControler().getScenario()).setVehicles(this.vehicles);
		this.config = new TransitRouterConfig(event.getControler().getScenario().getConfig().planCalcScore()
				, event.getControler().getScenario().getConfig().plansCalcRoute(), event.getControler().getScenario().getConfig().transitRouter(),
				event.getControler().getScenario().getConfig().vspExperimental());
		if(this.agentsStuckHandler != null){
			event.getControler().getEvents().addHandler(this.agentsStuckHandler);
		}
		this.dumpTransitScheduleAndVehicles(event.getControler());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		final Controler controler = event.getControler();
		if(event.getIteration() == 0){
			log.info("This is the first iteration. All lines were added by notifyStartup event.");
		} else {
			this.pBox.notifyIterationStarts(event);
			this.needToUpdateRouter = true;
			this.schedule = addPTransitScheduleToOriginalOne(this.baseSchedule, this.pBox.getpTransitSchedule());
			((PScenarioImpl) event.getControler().getScenario()).setTransitSchedule(this.schedule);
			this.vehicles = this.addPVehiclesToOriginalOnes(this.baseVehicles, this.pVehiclesFactory.createVehicles(this.pBox.getpTransitSchedule()));
			((PScenarioImpl) event.getControler().getScenario()).setVehicles(this.vehicles);
			
			if(this.agentsStuckHandler != null){
				ParallelPersonAlgorithmRunner.run(controler.getPopulation(), controler.getConfig().global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new PersonReRouteStuck(controler.createRoutingAlgorithm(), controler.getScenario(), agentsStuckHandler.getAgentsStuck());
					}
				});
			}
			this.dumpTransitScheduleAndVehicles(event.getControler());
		}
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
	
	private void dumpTransitScheduleAndVehicles(Controler controler){
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(schedule);
		VehicleWriterV1 writer2 = new VehicleWriterV1(controler.getScenario().getVehicles());
		
		if (controler.getIterationNumber() == null) {
			writer.write(controler.getControlerIO().getOutputFilename(controler.getFirstIteration() + ".transitSchedule.xml.gz"));
			writer2.writeFile(controler.getControlerIO().getOutputFilename(controler.getFirstIteration() + ".vehicles.xml.gz"));
		} else {
			writer.write(controler.getControlerIO().getIterationFilename(controler.getIterationNumber().intValue(), "transitSchedule.xml.gz"));
			writer2.writeFile(controler.getControlerIO().getIterationFilename(controler.getIterationNumber().intValue(), "vehicles.xml.gz"));
		}
	}

}