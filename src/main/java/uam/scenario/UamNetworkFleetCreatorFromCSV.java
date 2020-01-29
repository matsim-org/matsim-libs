/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package uam.scenario;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.MergeNetworks;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.common.collect.ImmutableSet;
import com.opencsv.CSVReader;

/**
 * @author Steffen Axer, based on Michal Maciejewski (michalm)
 */
public class UamNetworkFleetCreatorFromCSV {

	// String hubFile = "C://Users//VWBIDGN//Documents//ports.csv";
	// String inputNetwork =
	// "D://Matsim//Axer//Hannover//Base//vw280_0.1//vw280_0.1.output_network.xml.gz";
	// String outputFolder = "D://Matsim//Axer//Hannover//ZIM//input//uam";
	String hubFile;
	String inputNetwork;
	String outputFolder;
	List<String[]> CSVData;
	HashMap<Id<Link>, Integer> coord2PortsMap;
	final static String UAM_MODE = "uam";
	Network network;
	Network uamNetwork;
	Network filteredCarNetwork;
	public double weightIn = 0.7;
	public double weightOut = 0.3;
	TransitSchedule transitSchedule;
	Integer totalPorts;
	List<Node> skyNodes = new ArrayList<>();

	// static final ImmutableList<Id<Link>> SELECTED_LINK_IDS =
	// ImmutableList.of(Id.createLinkId(187463),
	// Id.createLinkId(174307), Id.createLinkId(356905), Id.createLinkId(224927),
	// Id.createLinkId(67436),
	// Id.createLinkId(341408));

	static final String HUB_LINK_ID_PREFIX = "HUB_";

	private final double UAV_EFFECTIVE_CELL_SIZE = 7.5;// [m]; see DEFAULT_EFFECTIVE_CELL_SIZE

	// XXX could be split into EGRESS and ACCESS links, each 2 min long
	private final double HUB_LINK_TRAVEL_TIME = 5 * 60;// [s]; 1 / 5-min frequency per port
	private final double HUB_LINK_LENGTH = UAV_EFFECTIVE_CELL_SIZE;// [m] only one UAV per lane at a time
	public double HUB_LINK_NUM_LANES = 5;// [lanes] = number of ports at the hub
	private final double HUB_LINK_SPEED = HUB_LINK_LENGTH / HUB_LINK_TRAVEL_TIME;// [m/s]
	private final double HUB_LINK_FLOW_CAPACITY = 3600. * HUB_LINK_NUM_LANES / HUB_LINK_TRAVEL_TIME;// 60 [veh/h]

	private final double RED_LINK_TRAVEL_TIME = 15;// [s]
	private final double RED_LINK_LENGTH = 150;// [m]
	private final double VERTICAL_SEPARATION = 100; // [m]

	/**
	 * NEED TO KNOW: RED_LINK_NUM_LANES defines implicitly the storage capacity of
	 * this link. As the storage capacity is calculated by LinkLength/CellSize *
	 * lanes, one need to down scale RED_LINK_NUM_LANES with the factor
	 * (UAV_EFFECTIVE_CELL_SIZE / RED_LINK_LENGTH) in order to reduce the effective
	 * storage capacity
	 */

	private final double RED_LINK_NUM_LANES = UAV_EFFECTIVE_CELL_SIZE / RED_LINK_LENGTH
			* (RED_LINK_LENGTH / VERTICAL_SEPARATION);// [lanes]; only 1 UAV at	a time
	private final double RED_LINK_SPEED = RED_LINK_LENGTH / RED_LINK_TRAVEL_TIME;// 5 [m/s]
	private final double RED_LINK_FLOW_CAPACITY = 3600. / RED_LINK_TRAVEL_TIME;// 120 [veh/h]

	private final double GREEN_LINK_SPEED = 108 / 3.6;// [m/s]; 108 km/h
	private final double GREEN_LINK_FLOW_CAPACITY = 60;// [veh/h]

	public UamNetworkFleetCreatorFromCSV(String hubFile, String inputNetwork, String outputFolder) {
		this.hubFile = hubFile;
		this.inputNetwork = inputNetwork;
		this.outputFolder = outputFolder;
		this.coord2PortsMap = new HashMap<Id<Link>, Integer>();
		// read network (CRS: ETRS89_UTM_zone_32N)
		this.network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(this.network).readFile(inputNetwork);

		NetworkFilterManager nfm = new NetworkFilterManager(this.network);
		nfm.addLinkFilter(new NetworkLinkFilter() {

			@Override
			public boolean judgeLink(Link l) {
				if (l.getAllowedModes().contains("car")) {
					return true;
				} else
					return false;
			}
		});

		this.filteredCarNetwork = nfm.applyFilters();

		this.uamNetwork = NetworkUtils.createNetwork();
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
	}

	public static void main(String[] args) {
		new UamNetworkFleetCreatorFromCSV("C://Users//VWBIDGN//Documents//ports.csv",
				"D://Matsim//Axer//Hannover//Base//vw280_0.1//vw280_0.1.output_network.xml.gz",
				"D://Matsim//Axer//Hannover//ZIM//input//uam").run(500);
	}

	public void run(Integer totalPorts) {
		this.totalPorts = totalPorts;
		readHubsCSV();
		Set<String> uamLinkModes = ImmutableSet.of(UAM_MODE);
		int i = 0;
		for (Entry<Id<Link>, Integer> entry : coord2PortsMap.entrySet()) {
			// TODO currently they are in the middle of each link
			Coord hubCoord = network.getLinks().get(entry.getKey()).getCoord();

			// create HUB landing and takeoff nodes
			Node hubLandingNode = NetworkUtils.createAndAddNode(uamNetwork, Id.createNodeId("HUB_LANDING_" + i),
					new Coord(hubCoord.getX() - 0.5 * HUB_LINK_LENGTH, hubCoord.getY()));
			Node hubTakeoffNode = NetworkUtils.createAndAddNode(uamNetwork, Id.createNodeId("HUB_TAKEOFF" + i),
					new Coord(hubCoord.getX() + 0.5 * HUB_LINK_LENGTH, hubCoord.getY()));

			// create SKY node that corresponds to the UAM flying level
			// XXX for better visibility in VIA, use increase Y by RED_LINK_LENGTH (instead
			// of Z)
			Coord skyCoord = new Coord(hubCoord.getX(), hubCoord.getY() + RED_LINK_LENGTH - 0.5 * HUB_LINK_LENGTH);
			Node skyNode = NetworkUtils.createAndAddNode(uamNetwork, Id.createNodeId("UAM_SKY_" + i), skyCoord);
			skyNodes.add(skyNode);

			// create RED links
			NetworkUtils
					.createAndAddLink(uamNetwork, Id.createLinkId("RED_LANDING_" + i), skyNode, hubLandingNode,
							RED_LINK_LENGTH, RED_LINK_SPEED, RED_LINK_FLOW_CAPACITY, RED_LINK_NUM_LANES)
					.setAllowedModes(uamLinkModes);
			NetworkUtils
					.createAndAddLink(uamNetwork, Id.createLinkId("RED_TAKEOFF_" + i), hubTakeoffNode, skyNode,
							RED_LINK_LENGTH, RED_LINK_SPEED, RED_LINK_FLOW_CAPACITY, RED_LINK_NUM_LANES)
					.setAllowedModes(uamLinkModes);

			// create access/egress link
			Link hubLink = NetworkUtils.createAndAddLink(uamNetwork, Id.createLinkId(HUB_LINK_ID_PREFIX + i),
					hubLandingNode, hubTakeoffNode, HUB_LINK_LENGTH, HUB_LINK_SPEED, HUB_LINK_FLOW_CAPACITY,
					HUB_LINK_NUM_LANES);
			hubLink.setAllowedModes(uamLinkModes);

			TransitStopFacility transitStop = transitSchedule.getFactory()
					.createTransitStopFacility(Id.create("STOP_HUB_" + i, TransitStopFacility.class), hubCoord, false);
			transitStop.setLinkId(hubLink.getId());
			transitSchedule.addStopFacility(transitStop);
			i++;
		}

		for (int k = 0; k < skyNodes.size(); k++) {
			Node fromNode = skyNodes.get(k);
			for (int j = 0; j < skyNodes.size(); j++) {
				if (k != j) {
					Node toNode = skyNodes.get(j);
					double length = DistanceUtils.calculateDistance(fromNode, toNode);
					NetworkUtils
							.createAndAddLink(uamNetwork, Id.createLinkId("GREEN_" + k + "_" + j), fromNode, toNode,
									length, GREEN_LINK_SPEED, GREEN_LINK_FLOW_CAPACITY, 1)
							.setAllowedModes(uamLinkModes);
				}
			}
		}

		new NetworkWriter(uamNetwork).write(outputFolder + "/uam_only_network.xml");
		MergeNetworks.merge(network, "", uamNetwork);
		new NetworkWriter(network).write(outputFolder + "/network_with_uam.xml.gz");

		new TransitScheduleWriter(transitSchedule).writeFile(outputFolder + "/uam_stops.xml");
		generateFleet();
	}

	public void readHubsCSV() {
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				"EPSG:25832");

		// "clusterId","demand","lon","lat","demandRel","dir"
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(hubFile));
			CSVData = reader.readAll();
			for (int i = 1; i < CSVData.size(); i++) {
				String[] lineContents = CSVData.get(i);
				double clusterId = Integer.parseInt(lineContents[0]); // clusterId,
				double demand = Double.parseDouble(lineContents[1]); // demand,
				double lat = Double.parseDouble(lineContents[2]); // lon
				double lon = Double.parseDouble(lineContents[3]); // lat
				double demandRel = Double.parseDouble(lineContents[4]); // demandRel
				String dir = lineContents[5]; // dir
				Integer uavFleetAtPort;
				if (dir.equals("in")) {
					uavFleetAtPort = (int) Math.round(demandRel * totalPorts * weightIn);
				} else if (dir.equals("out")) {
					uavFleetAtPort = (int) Math.round(demandRel * totalPorts * weightOut);
				} else {
					uavFleetAtPort = (int) (demandRel * totalPorts);
				}
				Coord transFormedCoord = ct.transform(new Coord(lat, lon));
				coord2PortsMap.put(NetworkUtils.getNearestLink(filteredCarNetwork, transFormedCoord).getId(),
						uavFleetAtPort);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	void generateFleet() {
		FleetSpecification fleet = new FleetSpecificationImpl();
		int i = 0;
		for (Entry<Id<Link>, Integer> entry : coord2PortsMap.entrySet()) {
			Id<Link> hub = Id.createLinkId(UamNetworkCreator.HUB_LINK_ID_PREFIX + i);

			int vehicles = entry.getValue().intValue();
			int capacity = 1;
			for (int j = 0; j < vehicles; j++) {
				Id<DvrpVehicle> vehicleId = Id.create("UAV_" + i + "_" + j, DvrpVehicle.class);
				DvrpVehicleSpecification vehicle = ImmutableDvrpVehicleSpecification.newBuilder().id(vehicleId)
						.capacity(capacity).serviceBeginTime(0).serviceEndTime(30 * 3600).startLinkId(hub).build();

				fleet.addVehicleSpecification(vehicle);
			}
			i++;
		}
		new FleetWriter(fleet.getVehicleSpecifications().values().stream()).write(outputFolder + "/uam_fleet.xml");

	}

}
