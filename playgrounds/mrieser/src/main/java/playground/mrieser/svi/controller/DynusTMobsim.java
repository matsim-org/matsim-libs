/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.mrieser.svi.data.DynamicODMatrix;
import playground.mrieser.svi.data.DynusTDynamicODDemandWriter;
import playground.mrieser.svi.data.vehtrajectories.CalculateLinkStatsFromVehTrajectories;
import playground.mrieser.svi.data.vehtrajectories.CalculateLinkTravelTimesFromVehTrajectories;
import playground.mrieser.svi.data.vehtrajectories.CalculateTravelTimeMatrixFromVehTrajectories;
import playground.mrieser.svi.data.vehtrajectories.DynamicTravelTimeMatrix;
import playground.mrieser.svi.data.vehtrajectories.MultipleVehicleTrajectoryHandler;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoriesReader;
import playground.mrieser.svi.replanning.DynamicODDemandCollector;

/**
 * @author mrieser
 */
public class DynusTMobsim implements Mobsim {

	private final static Logger log = Logger.getLogger(DynusTMobsim.class);

	private final DynusTConfig dc;
	private final Scenario scenario;
	private final DynamicTravelTimeMatrix ttMatrix;
	private final Network dynusTnet;
	private final Controler controler;

	public DynusTMobsim(final DynusTConfig dc, final DynamicTravelTimeMatrix ttMatrix, final Scenario sc, final EventsManager eventsManager,
			final Network dynusTnet, final Controler controler) {
		this.dc = dc;
		this.scenario = sc;
		this.ttMatrix = ttMatrix;
		this.dynusTnet = dynusTnet;
		this.controler = controler;
	}

	@Override
	public void run() {
		// prepare matrix
		log.info("collect demand for Dynus-T");
		DynamicODMatrix odm = new DynamicODMatrix(this.dc.getTimeBinSize_min()*60, 24*60*60);
		DynamicODDemandCollector collector = new DynamicODDemandCollector(odm, this.dc.getActToZoneMapping());

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			collector.run(plan);
		}
		log.info("Number of Trips handed over to DynusT: " + collector.getCounter());

		log.info("write demand for Dynus-T");
		DynusTDynamicODDemandWriter writer = new DynusTDynamicODDemandWriter(odm, this.dc.getZoneIdToIndexMapping());
		writer.setMultiplyFactor(this.dc.getDemandFactor());
		writer.writeTo(this.dc.getOutputDirectory() + "/demand.dat");

		// run DynusT
		log.info("run Dynus-T");
		DynusTExe exe = new DynusTExe(this.dc.getDynusTDirectory(), this.dc.getModelDirectory(), this.dc.getOutputDirectory());
		exe.runDynusT(true);

		// read in data, convert it somehow to score the plans
		log.info("read in Vehicle Trajectories from DynusT");
		String vehTrajFilename = this.dc.getOutputDirectory() + "/VehTrajectory.dat";
		
		MultipleVehicleTrajectoryHandler multiHandler = new MultipleVehicleTrajectoryHandler();
		CalculateTravelTimeMatrixFromVehTrajectories ttmCalc = new CalculateTravelTimeMatrixFromVehTrajectories(this.ttMatrix);
		multiHandler.addTrajectoryHandler(ttmCalc);
		TravelTimeCalculator ttc = new TravelTimeCalculator(this.dynusTnet, this.scenario.getConfig().travelTimeCalculator());
		CalculateLinkTravelTimesFromVehTrajectories lttCalc = new CalculateLinkTravelTimesFromVehTrajectories(ttc, this.dynusTnet);
		multiHandler.addTrajectoryHandler(lttCalc);
		CalculateLinkStatsFromVehTrajectories linkStats = new CalculateLinkStatsFromVehTrajectories(this.dynusTnet);
		multiHandler.addTrajectoryHandler(linkStats);
		
		new VehicleTrajectoriesReader(multiHandler, this.dc.getZoneIdToIndexMapping()).readFile(vehTrajFilename);

		this.dc.setTravelTimeCalculator(ttc);
		linkStats.writeLinkVolumesToFile(this.controler.getControlerIO().getIterationFilename(this.controler.getIterationNumber(), "dynust_linkVolumes.txt"));
		linkStats.writeLinkTravelTimesToFile(this.controler.getControlerIO().getIterationFilename(this.controler.getIterationNumber(), "dynust_linkTravelTimes.txt"));
		linkStats.writeLinkTravelSpeedsToFile(this.controler.getControlerIO().getIterationFilename(this.controler.getIterationNumber(), "dynust_linkTravelSpeeds.txt"));
	}

}
