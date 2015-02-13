/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.SAXException;
import playground.boescpa.converters.osm.networkCreator.MultimodalNetworkCreatorSimple;
import playground.boescpa.converters.osm.ptRouter.PTLineRouterDefaultV2;
import playground.boescpa.converters.osm.scheduleCreator.PTScheduleCreatorDefaultV2;
import playground.boescpa.lib.tools.cutter.ScheduleCutter;
import playground.christoph.evacuation.pt.TransitRouterNetworkReaderMatsimV1;
import playground.christoph.evacuation.pt.TransitRouterNetworkThinner;
import playground.christoph.evacuation.pt.TransitRouterNetworkWriter;
import playground.christoph.evacuation.pt.WriteTransitRouterNetwork;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class OSM2MixedV2 {

	private final static int maxBeelineWalkConnectingDistance = 1; // new TransitRouterConfig(ConfigUtils.createConfig()).beelineWalkConnectionDistance // = 100

	private static int cutRadius = 0;
	private static Coord cutCenter = null;
	private static String outbase = null;

	public static void main(String[] args) {
		if (args.length > 9 || args.length < 7) {
			System.out.println("The number of input arguments is wrong. Please check. Program will abort.");
			return;
		}

		// **************** Preparations ****************
		// Get an empty network and an empty schedule:
		final Scenario scenario = getEmptyScenario();
		final Network network = scenario.getNetwork();
		final TransitSchedule schedule = scenario.getTransitSchedule();
		final Vehicles vehicles = scenario.getVehicles();
		// Get resources:
		final String osmFile = args[0];
		final String hafasFolder = args[1];
		final String vehicleFile_Mixed = args[2];
		final String vehicleFile_OnlyPT = args[3];
		// Set spatial cutter
		cutCenter = new CoordImpl(Double.parseDouble(args[4]), Double.parseDouble(args[5]));
		cutRadius = Integer.parseInt(args[6]);
		// Prepare output
		final String outputFolder = args[7] + "\\";
		String outputPrefix = "";
		if (args.length == 9) {
			outputPrefix = args[8];
		}
		outbase = outputFolder + outputPrefix;
		final String networkPath = outbase + "Network.xml.gz";

		// **************** Prepare Subscenarios ****************
		//convertOSMNetwork(scenario, network, osmFile, networkPath);
		//final Scenario onlyPTScenario = createOnlyPT(hafasFolder, vehicleFile_OnlyPT);
		final Scenario mixedScenario = createMixed(hafasFolder, vehicleFile_Mixed, networkPath);



	}

	protected static Scenario createMixed(String hafasFolder, String vehicleFile_Mixed, String networkPath) {
		final Scenario mixedScenario = getEmptyScenario();

		// **************** Convert Schedule ****************
		final TransitSchedule mixedSchedule = mixedScenario.getTransitSchedule();
		final Vehicles mixedVehicles = VehicleUtils.createVehiclesContainer();
		new PTScheduleCreatorDefaultV2(mixedSchedule, mixedVehicles).createSchedule(null, hafasFolder, null, vehicleFile_Mixed);
		new TransitScheduleWriter(mixedSchedule).writeFile(outbase + "MixedSchedule_Plain.xml.gz");
		new VehicleWriterV1(mixedVehicles).writeFile(outbase + "MixedVehicles.xml.gz");

		// **************** Cut Schedule ****************
		new ScheduleCutter(mixedSchedule, cutCenter, cutRadius).cutSchedule();

		// **************** Route Schedule ****************
		new NetworkReaderMatsimV1(mixedScenario).parse(networkPath);
		final Network mixedNetwork = mixedScenario.getNetwork();
		new PTLineRouterDefaultV2(mixedSchedule).routePTLines(mixedNetwork);
		final String path_MixedSchedule = outbase + "MixedSchedule.xml.gz";
		final String path_MixedNetwork = outbase + "MixedNetwork.xml.gz";
		new TransitScheduleWriter(mixedSchedule).writeFile(path_MixedSchedule);
		new NetworkWriter(mixedNetwork).write(path_MixedNetwork);

		// **************** Thin Network ****************
		//final String path_ThinnedNetwork = thinTransitNetwork("Mixed", mixedSchedule);

		// **************** Validate Schedule ****************
		try {
			TransitScheduleValidator.main(new String[]{path_MixedSchedule, path_MixedNetwork});
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mixedScenario;
	}

	protected static Scenario createOnlyPT(String hafasFolder, String vehicleFile_OnlyPT) {
		final Scenario onlyPTScenario = getEmptyScenario();

		// **************** Convert Schedule ****************
		final TransitSchedule onlyPTSchedule = onlyPTScenario.getTransitSchedule();
		final Vehicles onlyPTVehicles = onlyPTScenario.getVehicles();
		new PTScheduleCreatorDefaultV2(onlyPTSchedule, onlyPTVehicles).createSchedule(null, hafasFolder, null, vehicleFile_OnlyPT);
		new TransitScheduleWriter(onlyPTSchedule).writeFile(outbase + "OnlyPTSchedule_Plain.xml.gz");
		new VehicleWriterV1(onlyPTVehicles).writeFile(outbase + "OnlyPTVehicles.xml.gz");

		// **************** Cut Schedule ****************
		new ScheduleCutter(onlyPTSchedule, cutCenter, cutRadius).cutSchedule();

		// **************** Route Schedule ****************
		final Network onlyPTNetwork = onlyPTScenario.getNetwork();
		new CreatePseudoNetwork(onlyPTSchedule, onlyPTNetwork, "pt_").createNetwork();
		final String path_OnlyPTSchedule = outbase + "OnlyPTSchedule.xml.gz";
		final String path_OnlyPTNetwork = outbase + "OnlyPTNetwork.xml.gz";
		new TransitScheduleWriter(onlyPTSchedule).writeFile(path_OnlyPTSchedule);
		new NetworkWriter(onlyPTNetwork).write(path_OnlyPTNetwork);

		// **************** Thin Network ****************
		//final String path_ThinnedNetwork = thinTransitNetwork("OnlyPT", onlyPTSchedule);

		// **************** Validate Schedule ****************
		try {
			TransitScheduleValidator.main(new String[]{path_OnlyPTSchedule, path_OnlyPTNetwork});
		} catch (Exception e) {
			e.printStackTrace();
		}

		return onlyPTScenario;
	}

	protected static String thinTransitNetwork(final String prefix, final TransitSchedule schedule) {
		final String path_TransitNetwork = outbase + prefix + "TransitNetwork";
		final String path_ThinnedNetwork = outbase + prefix + "Network_thinned.xml.gz";
		// Create transit network
		TransitRouterNetwork transitRouterNetwork = TransitRouterNetwork.createFromSchedule(schedule,
				maxBeelineWalkConnectingDistance);
		System.gc();
		// Write full transit network
		new TransitRouterNetworkWriter(transitRouterNetwork).write(path_TransitNetwork + "_full.xml.gz");
		// Thin transit network
		new TransitRouterNetworkThinner.RemoveRedundantLinks().run(transitRouterNetwork);
		new TransitRouterNetworkThinner.RemoveRedundantDistanceLinks().run(transitRouterNetwork);
		System.gc();
		// Write thinned transit network
		new TransitRouterNetworkWriter(transitRouterNetwork).write(path_TransitNetwork + "_thinned.xml.gz");
		new NetworkWriter(transitRouterNetwork).write(path_ThinnedNetwork);
		return path_ThinnedNetwork;
	}

	protected static void convertOSMNetwork(Scenario scenario, Network network, String osmFile, String networkPath) {
		new MultimodalNetworkCreatorSimple(scenario.getNetwork()).createMultimodalNetwork(osmFile);
		new NetworkWriter(network).write(networkPath);
	}

	protected static Scenario getEmptyScenario() {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		return scenario;
	}
}
