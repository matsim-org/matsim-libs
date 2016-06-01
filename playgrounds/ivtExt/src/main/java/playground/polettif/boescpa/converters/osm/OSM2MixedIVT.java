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

package playground.polettif.boescpa.converters.osm;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.run.NetworkCleaner;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import playground.polettif.boescpa.converters.osm.networkCreator.MMStreetNetworkCreatorFactory;
import playground.polettif.boescpa.converters.osm.ptMapping.PTMapperOnlyBusses;
import playground.polettif.boescpa.converters.osm.ptMapping.PseudoNetworkCreator;
import playground.polettif.boescpa.converters.osm.scheduleCreator.PTScheduleCreatorDefaultV2;
import playground.polettif.boescpa.converters.osm.tools.TransitRouterNetworkThinner;
import playground.polettif.boescpa.converters.osm.tools.TransitRouterNetworkWriter;
import playground.polettif.boescpa.lib.tools.coordUtils.CoordFilter;
import playground.polettif.boescpa.lib.tools.spatialCutting.ScheduleCutter;
import playground.polettif.boescpa.lib.tools.fileMerging.NetworkMerger;
import playground.polettif.boescpa.lib.tools.fileMerging.ScheduleMerger;
import playground.polettif.boescpa.lib.tools.fileMerging.VehicleMerger;

/**
 * Main to create multimodal MATSim environment from OSM and HAFAS.
 * Less straight forward to use but also more powerful than OSM2MixedConverter.
 *
 * @author boescpa
 */
public class OSM2MixedIVT {

	private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");
	private final static double maxBeelineWalkConnectingDistance =
			new TransitRouterConfig(ConfigUtils.createConfig()).getBeelineWalkConnectionDistance(); // = 100

	private static int cutRadius = 0;
	private static Coord cutCenter = null;
	private static Coord cutNW = null;
	private static Coord cutSE = null;
	private static String outbase = null;

	public static void main(String[] args) {
		if (args.length > 13 || args.length < 12) {
			System.out.println("The number of input arguments is wrong. Please check. Program will abort.");
			return;
		}

		// **************** Preparations ****************
		// Get resources:
		final String osmFile = args[0];
		final String hafasFolder = args[1];
		final String vehicleFile_Mixed = args[2];
		final String vehicleFile_OnlyPT = args[3];
		// Set spatial cutter
		cutCenter = new Coord(Double.parseDouble(args[4]), Double.parseDouble(args[5]));
		cutRadius = Integer.parseInt(args[6]);
		cutNW = new Coord(Double.parseDouble(args[7]), Double.parseDouble(args[8]));
		cutSE = new Coord(Double.parseDouble(args[9]), Double.parseDouble(args[10]));
		// Prepare output
		final String outputFolder = args[11];// + "\\";
		String outputPrefix = "";
		if (args.length == 13) {
			outputPrefix = args[12];
		}
		outbase = outputFolder + outputPrefix;
		final String networkPath = outbase + "Network.xml.gz";

		// **************** Prepare Subscenarios ****************
		convertOSMNetwork(osmFile, networkPath);
		final Scenario onlyPTScenario = createOnlyPT(hafasFolder, vehicleFile_OnlyPT);
		final Scenario mixedScenario = createMixed(hafasFolder, vehicleFile_Mixed, networkPath);

		// **************** Merge Subscenarios ****************
		final Network mergedNetwork = NetworkMerger.mergeNetworks(mixedScenario.getNetwork(), onlyPTScenario.getNetwork());
		final TransitSchedule mergedSchedule = ScheduleMerger.mergeSchedules(mixedScenario.getTransitSchedule(), onlyPTScenario.getTransitSchedule());
		final Vehicles mergedVehicles = VehicleMerger.mergeVehicles(mixedScenario.getVehicles(), onlyPTScenario.getVehicles());


		// **************** Write final scenario ****************
		final String path_FinalNetwork = outbase + "FinalNetwork.xml.gz";
		new NetworkWriter(mergedNetwork).write(path_FinalNetwork);
		final String path_FinalSchedule = outbase + "FinalSchedule.xml.gz";
		new TransitScheduleWriter(mergedSchedule).writeFile(path_FinalSchedule);
		new VehicleWriterV1(mergedVehicles).writeFile(outbase + "FinalVehicles.xml.gz");

		// **************** Validate final schedule ****************
		try {
			TransitScheduleValidator.main(new String[]{path_FinalSchedule, path_FinalNetwork});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	protected static Scenario createMixed(String hafasFolder, String vehicleFile_Mixed, String networkPath) {
		final Scenario mixedScenario = getEmptyScenario();

		// **************** Convert Schedule ****************
		final TransitSchedule mixedSchedule = mixedScenario.getTransitSchedule();
		final Vehicles mixedVehicles = mixedScenario.getVehicles();
		new PTScheduleCreatorDefaultV2(mixedSchedule, mixedVehicles).createSchedule(null, hafasFolder, null, vehicleFile_Mixed);
		new TransitScheduleWriter(mixedSchedule).writeFile(outbase + "MixedSchedule_Plain.xml.gz");
		new VehicleWriterV1(mixedVehicles).writeFile(outbase + "MixedVehicles.xml.gz");

		// **************** Cut Schedule ****************
		new ScheduleCutter(mixedSchedule, null, new CoordFilter.CoordFilterRectangle(
				transformation.transform(cutNW), transformation.transform(cutSE))).cutSchedule();

		// **************** Route Schedule ****************
		new NetworkReaderMatsimV1(mixedScenario.getNetwork()).parse(networkPath);
		final Network mixedNetwork = mixedScenario.getNetwork();
		new PTMapperOnlyBusses(mixedSchedule).routePTLines(mixedNetwork);
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
		new ScheduleCutter(onlyPTSchedule, null, new CoordFilter.CoordFilterRectangle(
				transformation.transform(cutNW), transformation.transform(cutSE))).cutSchedule();

		// **************** Route Schedule ****************
		final Network onlyPTNetwork = onlyPTScenario.getNetwork();
		new PseudoNetworkCreator(onlyPTSchedule, onlyPTNetwork, "pt_").createNetwork();
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

	protected static void convertOSMNetwork(String osmFile, String networkPath) {
		final Network network = getEmptyScenario().getNetwork();
		//MMStreetNetworkCreatorFactory.getCreatorRectangleAroundSwitzerland(network).createMultimodalNetwork(osmFile);
		MMStreetNetworkCreatorFactory.getCircleWithinRectangle(network, cutCenter, cutRadius, cutNW, cutSE).createMultimodalNetwork(osmFile);
		new NetworkWriter(network).write(networkPath);
		new NetworkCleaner().run(networkPath, networkPath);
	}

	protected static Scenario getEmptyScenario() {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		return scenario;
	}
}
