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

package playground.jbischoff.taxibus.scenario.trains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.VehiclesFactory;

/**
 * @author jbischoff
 *
 */
public class CreateStopsAndSchedule {
	public static void main(String[] args) {

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(
				"C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/versions/networkpt-feb.xml");
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(
				"C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/new/transitVehicles.xml");
		new TransitScheduleReader(scenario).readFile(
				"C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/new/bs-scheduleNetwork.xml");

		final TransitScheduleFactory transitScheduleFactory = scenario.getTransitSchedule().getFactory();
		final VehicleType type = new VehicleTypeImpl(Id.create("train", VehicleType.class));
		final VehiclesFactory vfact = scenario.getTransitVehicles().getFactory();
		type.setAccessTime(0.5);
		type.setEgressTime(0.5);
		type.setDescription("regional train");
		type.setDoorOperationMode(DoorOperationMode.parallel);
		VehicleCapacity cap = new VehicleCapacityImpl();
		cap.setSeats(500);
		cap.setStandingRoom(1000);
		type.setCapacity(cap);
		type.setLength(150);
		type.setWidth(3);
		scenario.getTransitVehicles().addVehicleType(type);

		String[] l = { "rb48n", "rb48s", "re30e", "re30w" };
		String folder = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/trains/";

		List<String> lines = new ArrayList<String>(Arrays.asList(l));

		for (final String lineString : lines) {
			System.out.println(lineString);
			TransitLine line = transitScheduleFactory.createTransitLine(Id.create(lineString, TransitLine.class));
			final List<Id<Link>> links = new ArrayList<>();
			final List<TransitRouteStop> stops = new ArrayList<>();

			String stopFile = folder + lineString + "_stops.txt";
			TabularFileParserConfig stopsConfig = new TabularFileParserConfig();
			stopsConfig.setDelimiterTags(new String[] { "\t" });
			stopsConfig.setFileName(stopFile);
			stopsConfig.setCommentTags(new String[] { "#" });
			new TabularFileParser().parse(stopsConfig, new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {
					Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(row[1]));
					Id<TransitStopFacility> stopId = Id.create(row[0] + "_" + lineString, TransitStopFacility.class);
					TransitStopFacility stopFacility = transitScheduleFactory.createTransitStopFacility(stopId,
							link.getCoord(), false);
					stopFacility.setLinkId(link.getId());
					stopFacility.setName(row[0] + " " + lineString);
					scenario.getTransitSchedule().addStopFacility(stopFacility);
					double arrivalDelay = Double.parseDouble(row[2]) * 60;
					double departureDelay = arrivalDelay + 60;
					try {
						departureDelay = arrivalDelay + Double.parseDouble(row[3]) * 60;
					} catch (Exception e) {
					}

					TransitRouteStop stop = transitScheduleFactory.createTransitRouteStop(stopFacility, arrivalDelay,
							departureDelay);
					stop.setAwaitDepartureTime(true);
					stops.add(stop);
				}
			}

			);
			String routeFile = folder + lineString + "_line.txt";
			TabularFileParserConfig lineConfig = new TabularFileParserConfig();
			lineConfig.setDelimiterTags(new String[] { "\t" });
			lineConfig.setFileName(routeFile);
			lineConfig.setCommentTags(new String[] { "#" });
			new TabularFileParser().parse(lineConfig, new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {
					links.add(Id.createLinkId(row[0]));
				}
			});

			Id<Link> start = links.get(0);
			Id<Link> end = links.get(links.size() - 1);
			links.remove(start);
			links.remove(end);
			NetworkRoute route = new LinkNetworkRouteImpl(start, links, end);
			final TransitRoute troute = transitScheduleFactory
					.createTransitRoute(Id.create(lineString, TransitRoute.class), route, stops, "pt");

			String departuresFile = folder + lineString + "_departures.txt";
			TabularFileParserConfig depConfig = new TabularFileParserConfig();
			depConfig.setDelimiterTags(new String[] { "\t" });
			depConfig.setFileName(departuresFile);
			depConfig.setCommentTags(new String[] { "#" });
			new TabularFileParser().parse(depConfig, new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {

					double departureTime = Time.parseTime(row[0]);
					Vehicle veh = vfact.createVehicle(
							Id.create(lineString + "_" + Math.round(departureTime), Vehicle.class), type);
					scenario.getTransitVehicles().addVehicle(veh);

					Id<Departure> depId = Id.create(lineString + "_" + Math.round(departureTime), Departure.class);
					Departure departure = transitScheduleFactory.createDeparture(depId, departureTime);
					departure.setVehicleId(veh.getId());
					troute.addDeparture(departure);
				}
			});
			line.addRoute(troute);
			scenario.getTransitSchedule().addTransitLine(line);
		}
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(folder + "transitschedule.xml");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(folder + "transitvehicles.xml");
		ValidationResult vr = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(),
				scenario.getNetwork());
		System.out.println(vr.isValid());
		for (String s : vr.getErrors()) {
			System.out.println(s);
		}
	}

}
