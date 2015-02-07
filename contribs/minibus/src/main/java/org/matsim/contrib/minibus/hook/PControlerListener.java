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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.util.HashSet;
import java.util.Set;

/**
 * Hook to register paratransit black box with MATSim
 * 
 * @author aneumann
 */
final class PControlerListener implements IterationStartsListener, StartupListener, ScoringListener {
	
	private final static Logger log = Logger.getLogger(PControlerListener.class);

	private final PTransitRouterFactory pTransitRouterFactory;
	private final PVehiclesFactory pVehiclesFactory;
	
	private final AgentsStuckHandlerImpl agentsStuckHandler;
	private final PBox pBox;

    private final PersonReRouteStuckFactory stuckFactory;

    PControlerListener(Controler controler, PBox pBox, PTransitRouterFactory pTransitRouterFactory, PersonReRouteStuckFactory stuckFactory, AgentsStuckHandlerImpl agentsStuckHandler){
        PConfigGroup pConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PConfigGroup.GROUP_NAME, PConfigGroup.class);
        this.pTransitRouterFactory = pTransitRouterFactory;
		this.pVehiclesFactory = new PVehiclesFactory(pConfig);
        this.agentsStuckHandler = agentsStuckHandler;
        this.stuckFactory = stuckFactory;
		this.pBox = pBox;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.pBox.notifyStartup(event);
        addPTransitScheduleToOriginalOne(event.getControler().getScenario().getTransitSchedule(), this.pBox.getpTransitSchedule());
		addPVehiclesToOriginalOnes(event.getControler().getScenario().getTransitVehicles(), this.pVehiclesFactory.createVehicles(this.pBox.getpTransitSchedule()));

		this.pTransitRouterFactory.createTransitRouterConfig(event.getControler().getConfig());
		this.pTransitRouterFactory.updateTransitSchedule(event.getControler().getScenario().getTransitSchedule());
		
		if(this.agentsStuckHandler != null){
			event.getControler().getEvents().addHandler(this.agentsStuckHandler);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		final Controler controler = event.getControler();
		if(event.getIteration() == controler.getConfig().controler().getFirstIteration()){
			log.info("This is the first iteration. All lines were added by notifyStartup event.");
		} else {
			this.pBox.notifyIterationStarts(event);
            removePreviousPTransitScheduleFromOriginalOne(event.getControler().getScenario().getTransitSchedule());
			addPTransitScheduleToOriginalOne(event.getControler().getScenario().getTransitSchedule(), this.pBox.getpTransitSchedule());
			removePreviousPVehiclesFromScenario(event.getControler().getScenario().getTransitVehicles());
            addPVehiclesToOriginalOnes(event.getControler().getScenario().getTransitVehicles(), this.pVehiclesFactory.createVehicles(this.pBox.getpTransitSchedule()));

			this.pTransitRouterFactory.updateTransitSchedule(event.getControler().getScenario().getTransitSchedule());
			
			if(this.agentsStuckHandler != null){
                ParallelPersonAlgorithmRunner.run(controler.getScenario().getPopulation(), controler.getConfig().global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return stuckFactory.getReRouteStuck(new PlanRouter(
						controler.getTripRouterProvider().get(),
						controler.getScenario().getActivityFacilities()
						), ((ScenarioImpl)controler.getScenario()), agentsStuckHandler.getAgentsStuck());
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
	
	private void dumpTransitScheduleAndVehicles(Controler controler, int iteration){
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(controler.getScenario().getTransitSchedule());
		VehicleWriterV1 writer2 = new VehicleWriterV1(controler.getScenario().getTransitVehicles());
		writer.write(controler.getControlerIO().getIterationFilename(iteration, "transitSchedule.xml.gz"));
		writer2.writeFile(controler.getControlerIO().getIterationFilename(iteration, "vehicles.xml.gz"));
	}
}