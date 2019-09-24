/* *********************************************************************** *
 * project: org.matsim.*
 * OSMReader.java
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

package org.matsim.contrib.signals.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.consistency.LanesAndSignalsCleaner;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesFactory;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.lanes.data.LanesWriter;
import org.xml.sax.Attributes;



public class RunNilsOSMSignalReader{
    public static void main(string[] args){
        String inputOSM = "../../../shared-svn/studies/tthunig/osmData/interpreter.osm";
        String outputDir = "../../../shared-svn/studies/tthunig/osmData/signalsAndLanesReader/cottbusCity/";
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
                TransformationFactory.WGS84_UTM33N);

        // create a config
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
            signalSystemsConfigGroup.setUseSignalSystems(true);
            config.qsim().setUseLanes(true);

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
            scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        // pick network, lanes and signals data from the scenario
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        Lanes lanes = scenario.getLanes();
        Network network = scenario.getNetwork();

        OsmNetworkWithLanesAndSignalsReader reader = new OsmNetworkWithLanesAndSignalsReader(network, ct, signalsData, lanes);
		reader.setAssumptions(false, // minimize small roundabouts
                false, // merge oneway Signal Systems
                false, // use radius reduction
                true, // allow U-turn at left lane only
                true, // make pedestrian signals
                false); // accept 4+ crossings
		reader.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639); // setting Bounding Box for signals and lanes
    // (south,west,north,east)
		reader.parse(inputOSM);
		reader.stats();

    /*
     * Clean the Network. Cleaning means removing disconnected components, so that
     * afterwards there is a route from every link to every other link. This may not
     * be the case in the initial network converted from OpenStreetMap.
     */

		new NetworkCleaner().run(network);
		new LanesAndSignalsCleaner().run(scenario);

    /*
     * Write the files out: network, lanes, signalSystems, signalGroups,
     * signalControl
     */

		new NetworkWriter(network).write(outputDir + "network.xml");
		new LanesWriter(lanes).write(outputDir + "lanes.xml");
        SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		signalsWriter.setSignalSystemsOutputFilename(outputDir + "signalSystems.xml");
		signalsWriter.setSignalGroupsOutputFilename(outputDir + "signalGroups.xml");
		signalsWriter.setSignalControlOutputFilename(outputDir + "signalControl.xml");
		signalsWriter.writeSignalsData(scenario);
    }

}