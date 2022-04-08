/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.runSignalsAndLanesOsmNetworkReader;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.consistency.LanesAndSignalsCleaner;
import org.matsim.contrib.signals.network.SignalsAndLanesOsmNetworkReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesWriter;

/**
 * @author tthunig
 *
 */
public class RunSignalsAndLanesOsmNetworkReader {

	/**
	 * @param args first argument input OSM file, second argument output directory
	 */
	public static void main(String[] args) {
		
		String inputOSM = "myOsmFile.osm";
		String outputDir = "myOutputDir/";
		if (args != null && args.length > 1) {
			inputOSM = args[0];
			outputDir = args[1];
		}
		
		// ** adapt this according to your scenario **
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				TransformationFactory.WGS84_UTM33N);

		// create a config
		Config config = ConfigUtils.createConfig();
		SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalSystemsConfigGroup.setUseSignalSystems(true);
		config.qsim().setUseLanes(true);

		// create a scenario
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		// pick network, lanes and signals data from the scenario
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		Lanes lanes = scenario.getLanes();
		Network network = scenario.getNetwork();

		// create and configure the signals and lanes osm reader
		SignalsAndLanesOsmNetworkReader reader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);
		reader.setMergeOnewaySignalSystems(false);
		reader.setAllowUTurnAtLeftLaneOnly(true);
		reader.setMakePedestrianSignals(false);
		
        // set bounding box for signals and lanes (south, west, north, east)
		// ** adapt this according to your scenario **
		reader.setBoundingBox(52.448, 13.23, 52.57, 13.5); // this is berlin
		
		// create network, lanes and signal data
		reader.parse(inputOSM);

        // Simplify the network except the junctions with signals as this might mess up already created plans
		NetworkSimplifier netSimplify = new NetworkSimplifier();
		netSimplify.setNodesNotToMerge(reader.getNodesNotToMerge());
		netSimplify.run(network);

        /*
         * Clean the Network. Cleaning means removing disconnected components, so that
         * afterwards there is a route from every link to every other link. This may not
         * be the case in the initial network converted from OpenStreetMap.
         */
		new NetworkCleaner().run(network);
		new LanesAndSignalsCleaner().run(scenario);

		// write the files out
		new NetworkWriter(network).write(outputDir + "network.xml");
		new LanesWriter(lanes).write(outputDir + "lanes.xml");
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(outputDir + "signalSystems.xml");
		signalsWriter.setSignalGroupsOutputFilename(outputDir + "signalGroups.xml");
		signalsWriter.setSignalControlOutputFilename(outputDir + "signalControl.xml");
		signalsWriter.writeSignalsData(scenario);
	}

}
