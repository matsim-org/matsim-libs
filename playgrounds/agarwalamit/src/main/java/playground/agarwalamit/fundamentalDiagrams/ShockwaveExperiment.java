/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.fundamentalDiagrams;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

public class ShockwaveExperiment {
	
	private static final String RUN_DIR = FileUtils.SHARED_SVN+"/projects/mixedTraffic/triangularNetwork/run313/singleModes/holes/car_SW/";

	public static void main(String[] args) {

		boolean isUsingOTFVis = false;
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setTimeVariantNetwork(true);

		List<String> mainModes = Arrays.asList("car");
		// queue model parameters
		QSimConfigGroup qSimConfigGroup = scenario.getConfig().qsim();
		qSimConfigGroup.setMainModes(mainModes);
		qSimConfigGroup.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
		qSimConfigGroup.setLinkDynamics(LinkDynamics.FIFO);
		qSimConfigGroup.setEndTime(24*3600.);
        qSimConfigGroup.setUsingFastCapacityUpdate(true);

		scenario.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		scenario.getConfig().qsim().setSnapshotPeriod(1.0);
		scenario.getConfig().controler().setSnapshotFormat(Arrays.asList( "transims", "otfvis" ));
		scenario.getConfig().qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);

		Vehicles vehicles = scenario.getVehicles();
		{
			VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
			car.setPcuEquivalents(1.0);
			car.setMaximumVelocity(60.0/3.6);
			vehicles.addVehicleType(car);
		}

		scenario.getConfig().controler().setOutputDirectory(RUN_DIR);

		//set flow capacity of the base link to zero for 1 min.
		scenario.getConfig().qsim().setStuckTime(10*3600);

		if (! isUsingOTFVis ) { //necessary to avoid placement on link/lane (2-D) if using the data to plot only one-D space.
			scenario.getConfig().qsim().setLinkWidthForVis((float)0);
			scenario.getNetwork().setEffectiveLaneWidth(0.);
		}

		ScenarioUtils.loadScenario(scenario);
		Link desiredLink = scenario.getNetwork().getLinks().get(Id.createLinkId(2));//baseLink is not chosen to observe some spillover

		double flowCapBefore = desiredLink.getCapacity();
		{
			NetworkChangeEvent event = new NetworkChangeEvent(35.*60.) ;
			event.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 0.0)); 
			event.addLink(desiredLink);
            NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), event);
		}
		{
			NetworkChangeEvent event = new NetworkChangeEvent(35.*60.+60*5) ;
			event.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, flowCapBefore/3600.)); // value should be in pcu/s
			event.addLink(desiredLink);
            NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), event);
		}

		RaceTrackLinkProperties raceTrackLinkProperties = new RaceTrackLinkProperties(1000.0, 1600.0,
				60.0/3.6, 1.0, new HashSet<>(mainModes));

		FundamentalDiagramDataGenerator fundamentalDiagramDataGenerator = new FundamentalDiagramDataGenerator(raceTrackLinkProperties, scenario);
		fundamentalDiagramDataGenerator.setModalShareInPCU(new Double [] {1.0});
		fundamentalDiagramDataGenerator.setReduceDataPointsByFactor(10);
		fundamentalDiagramDataGenerator.setIsWritingEventsFileForEachIteration(true);
		fundamentalDiagramDataGenerator.setIsPlottingDistribution(false);
		fundamentalDiagramDataGenerator.setIsUsingLiveOTFVis(isUsingOTFVis);
		fundamentalDiagramDataGenerator.run();
	}
}
