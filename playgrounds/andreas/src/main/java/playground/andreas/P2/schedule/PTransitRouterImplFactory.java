/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeCost;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.osmBB.extended.TransitScheduleImpl;

/**
 * Hook to register paratransit black box with MATSim
 * 
 * @author aneumann
 */
public class PTransitRouterImplFactory implements TransitRouterFactory, IterationStartsListener, IterationEndsListener, StartupListener{
	
	private final static Logger log = Logger.getLogger(PTransitRouterImplFactory.class);

	private TransitSchedule baseSchedule;
	private TransitSchedule schedule;
	private TransitRouterConfig config;
	private TransitRouterNetwork routerNetwork;

	private PBox pBox;
	private Controler controler;

	public PTransitRouterImplFactory(Controler controler) {
		this.pBox = new PBox((PConfigGroup) controler.getConfig().getModule(PConfigGroup.GROUP_NAME));
		this.controler = controler;
	}

	@Override
	public TransitRouter createTransitRouter() {
		if(this.routerNetwork == null){
			this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.beelineWalkConnectionDistance);
		}		
		return new TransitRouterImpl(this.schedule, this.config, new TransitRouterNetworkTravelTimeCost(this.config), this.routerNetwork);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.pBox.init(controler);
		
		this.routerNetwork = null;
		this.baseSchedule = event.getControler().getScenario().getTransitSchedule();
		this.schedule = addPTransitScheduleToOriginalOne(new PTransitSchedule(this.baseSchedule), this.pBox.replan(event.getControler(), 0));
		((PScenarioImpl) this.controler.getScenario()).setTransitSchedule(this.schedule);
		addPVehiclesToOriginalOnes(event.getControler());
		this.config = new TransitRouterConfig(event.getControler().getScenario().getConfig().planCalcScore()
				, event.getControler().getScenario().getConfig().plansCalcRoute(), event.getControler().getScenario().getConfig().transitRouter(),
				event.getControler().getScenario().getConfig().vspExperimental());
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if(event.getIteration() == 0){
			log.info("This is the first iteration. All lines were added by notifyStartup event.");
		} else {
			this.pBox.reset(event);
			this.routerNetwork = null;
			this.schedule = addPTransitScheduleToOriginalOne(new PTransitSchedule(this.baseSchedule), this.pBox.replan(event.getControler(), event.getIteration()));
			((PScenarioImpl) this.controler.getScenario()).setTransitSchedule(this.schedule);
			addPVehiclesToOriginalOnes(event.getControler());
			
			TransitActsRemover transitActsRemover = new TransitActsRemover();
			for (Person person : this.controler.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					transitActsRemover.run(plan);
				}
				// TODO AN Add BeforeMobsimListener and only reroute the selected plan - speedup
//				transitActsRemover.run(person.getSelectedPlan());
			}

			ParallelPersonAlgorithmRunner.run(this.controler.getPopulation(), this.controler.getConfig().global().getNumberOfThreads(),
					new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
				@Override
				public AbstractPersonAlgorithm getPersonAlgorithm() {
					return new PersonPrepareForSim(controler.createRoutingAlgorithm(), controler.getNetwork());
				}
			});

			dumpTransitScheduleAndVehicles(event.getIteration());
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.pBox.score(event);		
	}

	private TransitSchedule addPTransitScheduleToOriginalOne(PTransitSchedule baseSchedule, TransitSchedule pSchedule) {
		
		TransitSchedule schedule = new TransitScheduleImpl(baseSchedule.getFactory());
		
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
	
	private void addPVehiclesToOriginalOnes(Controler controler){
		Vehicles pVeh = pBox.getVehicles();
		controler.getScenario().getVehicles().getVehicleTypes().putAll(pVeh.getVehicleTypes());
		controler.getScenario().getVehicles().getVehicles().putAll(pVeh.getVehicles());
	}
	
	private void dumpTransitScheduleAndVehicles(int iteration){
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(schedule);
		writer.write(this.controler.getControlerIO().getIterationFilename(iteration, "transitSchedule.xml.gz"));
		
		VehicleWriterV1 writer2 = new VehicleWriterV1(this.controler.getScenario().getVehicles());
		writer2.writeFile(this.controler.getControlerIO().getIterationFilename(iteration, "vehicles.xml.gz"));
	}

}