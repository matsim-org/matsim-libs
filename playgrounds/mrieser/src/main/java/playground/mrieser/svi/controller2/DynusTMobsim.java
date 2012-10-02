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

package playground.mrieser.svi.controller2;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;

import playground.mrieser.svi.data.DynamicODMatrix;
import playground.mrieser.svi.data.DynusTDynamicODDemandWriter;
import playground.mrieser.svi.data.vehtrajectories.CalculateTravelTimeMatrixFromVehTrajectories;
import playground.mrieser.svi.data.vehtrajectories.DynamicTravelTimeMatrix;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoriesReader;
import playground.mrieser.svi.replanning.DynamicODDemandCollector;

/**
 * @author mrieser
 */
public class DynusTMobsim implements Mobsim {

	private final DynusTConfig dc;
	private final Scenario scenario;
	private final DynamicTravelTimeMatrix ttMatrix;

	public DynusTMobsim(final DynusTConfig dc, final DynamicTravelTimeMatrix ttMatrix, final Scenario sc, final EventsManager eventsManager) {
		this.dc = dc;
		this.scenario = sc;
		this.ttMatrix = ttMatrix;
	}

	@Override
	public void run() {
		// prepare matrix
		DynamicODMatrix odm = new DynamicODMatrix(this.dc.getTimeBinSize_min()*60, 24*60*60);
		DynamicODDemandCollector collector = new DynamicODDemandCollector(odm, this.dc.getActToZoneMapping());

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			collector.run(plan);
		}

		DynusTDynamicODDemandWriter writer = new DynusTDynamicODDemandWriter(odm, this.dc.getZoneIdToIndexMapping());
		writer.setMultiplyFactor(this.dc.getDemandFactor());
		writer.writeTo(this.dc.getOutputDirectory() + "/demand.dat");

		// run DynusT
		DynusTExe exe = new DynusTExe(this.dc.getDynusTDirectory(), this.dc.getModelDirectory(), this.dc.getOutputDirectory());
		exe.runDynusT(true);

		// read in data, convert it somehow to score the plans
		String vehTrajFilename = this.dc.getOutputDirectory() + "/VehTrajectory.dat";
		CalculateTravelTimeMatrixFromVehTrajectories ttmCalc = new CalculateTravelTimeMatrixFromVehTrajectories(this.ttMatrix);
		new VehicleTrajectoriesReader(ttmCalc, this.dc.getZoneIdToIndexMapping()).readFile(vehTrajFilename);
		// TODO do something useful with matrix, i.e. pass it on to scoring etc.
	}

}
