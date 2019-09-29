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

package org.matsim.contrib.minibus.hook;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.operator.POperators;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.pt.router.TransitScheduleChangedEvent;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;


/**
 * Hook to register paratransit black box with MATSim
 * 
 * @author aneumann
 */
final class PControlerListener implements IterationStartsListener, StartupListener, ScoringListener {

	private final static Logger log = Logger.getLogger(PControlerListener.class);

	private final PVehiclesFactory pVehiclesFactory;

	@Inject(optional=true) private AgentsStuckHandlerImpl agentsStuckHandler;
	private final POperators operators ;

	@Inject(optional=true) private PersonReRouteStuckFactory stuckFactory;

	@Inject PControlerListener(Config config, POperators operators ){
		PConfigGroup pConfig = ConfigUtils.addOrGetModule(config, PConfigGroup.GROUP_NAME, PConfigGroup.class);
		this.pVehiclesFactory = new PVehiclesFactory(pConfig);
		this.operators = operators ;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		PBox pBox = (PBox) operators ;
		pBox.notifyStartup(event);
		addPTransitScheduleToOriginalOne(event.getServices().getScenario().getTransitSchedule(), pBox.getpTransitSchedule());
		addPVehiclesToOriginalOnes(event.getServices().getScenario().getTransitVehicles(), this.pVehiclesFactory.createVehicles(pBox.getpTransitSchedule()));
		event.getServices().getEvents().processEvent(new TransitScheduleChangedEvent(0.0));
		if(this.agentsStuckHandler != null){
			event.getServices().getEvents().addHandler(this.agentsStuckHandler);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		PBox pBox = (PBox) operators ;
		final MatsimServices controler = event.getServices();
		if(event.getIteration() == controler.getConfig().controler().getFirstIteration()){
			log.info("This is the first iteration. All lines were added by notifyStartup event.");
		} else {
			pBox.notifyIterationStarts(event);
			removePreviousPTransitScheduleFromOriginalOne(event.getServices().getScenario().getTransitSchedule());
			addPTransitScheduleToOriginalOne(event.getServices().getScenario().getTransitSchedule(), pBox.getpTransitSchedule());
			removePreviousPVehiclesFromScenario(event.getServices().getScenario().getTransitVehicles());
			addPVehiclesToOriginalOnes(event.getServices().getScenario().getTransitVehicles(), this.pVehiclesFactory.createVehicles(pBox.getpTransitSchedule()));
			event.getServices().getEvents().processEvent(new TransitScheduleChangedEvent(0.0));
			if(this.agentsStuckHandler != null){
				ParallelPersonAlgorithmUtils.run(controler.getScenario().getPopulation(), controler.getConfig().global().getNumberOfThreads(), new ParallelPersonAlgorithmUtils.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return stuckFactory.getReRouteStuck(new PlanRouter(
								controler.getTripRouterProvider().get(),
								controler.getScenario().getActivityFacilities()
								), ((MutableScenario)controler.getScenario()), agentsStuckHandler.getAgentsStuck());
					}
				});
			}
		}
		this.dumpTransitScheduleAndVehicles(event.getServices(), event.getIteration());
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		PBox pBox = (PBox) operators ;
		pBox.notifyScoring(event);
	}

	private final Set<Id<TransitStopFacility>> currentExclusivePFacilityIDs = new HashSet<>();
	private final Set<Id<TransitLine>> currentExclusivePTransitLineIDs = new HashSet<>();

	private void addPTransitScheduleToOriginalOne(TransitSchedule baseSchedule, TransitSchedule pSchedule) {
		if(pSchedule == null){
			log.info("pSchedule does not exist, doing nothing");
			return;
		}
		for (TransitStopFacility pStop : pSchedule.getFacilities().values()) {
			if (!baseSchedule.getFacilities().containsKey(pStop.getId())) {
				baseSchedule.addStopFacility(pStop);
				currentExclusivePFacilityIDs.add(pStop.getId());
			}
		}
		for (TransitLine pLine : pSchedule.getTransitLines().values()) {
			if (!baseSchedule.getTransitLines().containsKey(pLine.getId())) {
				baseSchedule.addTransitLine(pLine);
				currentExclusivePTransitLineIDs.add(pLine.getId());
			}
		}
	}

	private void removePreviousPTransitScheduleFromOriginalOne(TransitSchedule transitSchedule) {
		for (Id<TransitLine> transitLineId : currentExclusivePTransitLineIDs) {
			transitSchedule.removeTransitLine(transitSchedule.getTransitLines().get(transitLineId));
		}
		currentExclusivePTransitLineIDs.clear();
		for (Id<TransitStopFacility> facilityId : currentExclusivePFacilityIDs) {
			transitSchedule.removeStopFacility(transitSchedule.getFacilities().get(facilityId));
		}
		currentExclusivePFacilityIDs.clear();
	}

	private final Set<Id<VehicleType>> currentExclusivePVehicleTypeIDs = new HashSet<>();
	private final Set<Id<Vehicle>> currentExclusivePVehicleIDs = new HashSet<>();

	private void addPVehiclesToOriginalOnes(Vehicles baseVehicles, Vehicles pVehicles){
		for (VehicleType t : pVehicles.getVehicleTypes().values()) {
			if (!baseVehicles.getVehicleTypes().containsKey(t.getId())) {
				baseVehicles.addVehicleType(t);
				currentExclusivePVehicleTypeIDs.add(t.getId());
			}
		}
		for (Vehicle v : pVehicles.getVehicles().values()) {
			if (!baseVehicles.getVehicles().containsKey(v.getId())) {
				baseVehicles.addVehicle(v);
				currentExclusivePVehicleIDs.add(v.getId());
			}
		}
	}

	private void removePreviousPVehiclesFromScenario(Vehicles vehicles) {
		for (Id<Vehicle> vehicleId : currentExclusivePVehicleIDs) {
			vehicles.removeVehicle(vehicleId);
		}
		currentExclusivePVehicleIDs.clear();
		for (Id<VehicleType> vehicleTypeId : currentExclusivePVehicleTypeIDs) {
			vehicles.removeVehicleType(vehicleTypeId);
		}
		currentExclusivePVehicleTypeIDs.clear();
	}

	private void dumpTransitScheduleAndVehicles(MatsimServices controler, int iteration){
		TransitScheduleWriter writer = new TransitScheduleWriter(controler.getScenario().getTransitSchedule());
		MatsimVehicleWriter writer2 = new MatsimVehicleWriter(controler.getScenario().getTransitVehicles());
		writer.writeFile(controler.getControlerIO().getIterationFilename(iteration, "transitSchedule.xml.gz"));
		try {
			writer2.writeFile(controler.getControlerIO().getIterationFilename(iteration, "transitVehicles.xml.gz"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}