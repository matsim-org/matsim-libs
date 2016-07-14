/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.evaluation;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.vividsolutions.jts.geom.*;

import playground.jbischoff.utils.JbUtils;

/**
 * @author jbischoff
 *
 */
public class ZoneBasedTaxiStatusAnalysis implements LinkEnterEventHandler, ActivityEndEventHandler {

	Map<Id<Vehicle>, Boolean> vehicleOccupiedStatus = new HashMap<>();
	Map<Id<Vehicle>, Double> currentPickupDistance = new HashMap<>();
	Map<Id<Vehicle>, Double> currentPickupTime = new HashMap<>();

	Map<Id<Vehicle>, Double> currentDropoffDistance = new HashMap<>();
	Map<Id<Vehicle>, Double> currentOccupiedTripStartTime = new HashMap<>();

	Map<Id<Link>, Long> emptyRides = new HashMap<>();
	Map<Id<Link>, Long> occupiedRides = new HashMap<>();
	Map<String, double[]> zonesUsage = new HashMap<>();
	Map<String, Double> zonePickUpDistance = new HashMap<>();
	Map<String, Double> zoneRides = new HashMap<>();

	final int timebins = 24;
	double[] pickupDurationPerHour = new double[timebins];
	double[] dropoffDurationPerHour = new double[timebins];
	double[] pickupDistancePerHour = new double[timebins];
	double[] dropoffDistancePerHour = new double[timebins];
	double[] ridesPerHour = new double[timebins];
	// n.B. A taxi request set off at 14:50, arriving at customer at 15:01 would
	// be counted to happen at 14:00 hours

	private final Network network;
	private final Map<String, Geometry> zones;

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public ZoneBasedTaxiStatusAnalysis(Network network, Map<String, Geometry> zones) {
		this.network = network;
		this.zones = zones;
		for (Id<Link> linkId : network.getLinks().keySet()) {
			emptyRides.put(linkId, new Long(0));
			occupiedRides.put(linkId, new Long(0));
		}
		for (String zone : zones.keySet()) {
			zonesUsage.put(zone, new double[4]);
			zonePickUpDistance.put(zone, 0.);
			zoneRides.put(zone, 0.);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().startsWith("Stay")) {
			handleStayEvent(event);
		} else if (event.getActType().startsWith("PassengerPickup")) {
			handlePickupEvent(event);
		} else if (event.getActType().startsWith("Before schedule")) {
			handleBeforeSchedule(event);
		} else if (event.getActType().startsWith("PassengerDropoff")) {
			handleDropoffEvent(event);
		}

	}

	private void handleDropoffEvent(ActivityEndEvent event) {
		double pickUpTime = this.currentPickupTime.remove(p2vid(event.getPersonId()));
		int timebin = getTimeBin(pickUpTime);
		double dropOffTime = event.getTime();
		double dropOffDuration =  dropOffTime - this.currentOccupiedTripStartTime.remove(p2vid(event.getPersonId())) ;
		double dropOffDistance = currentDropoffDistance.get(p2vid(event.getPersonId()));
		this.dropoffDistancePerHour[timebin] += dropOffDistance;
		this.dropoffDurationPerHour[timebin] += dropOffDuration;
		this.ridesPerHour[timebin]++;
		this.currentDropoffDistance.put(p2vid(event.getPersonId()), 0.0);

	}

	private void handleBeforeSchedule(ActivityEndEvent event) {
		vehicleOccupiedStatus.put(p2vid(event.getPersonId()), false);
		this.currentPickupDistance.put(p2vid(event.getPersonId()), 0.0);
		this.currentDropoffDistance.put(p2vid(event.getPersonId()), 0.0);
	}

	private void handlePickupEvent(ActivityEndEvent event) {
		vehicleOccupiedStatus.put(p2vid(event.getPersonId()), true);
		double pUdistance = this.currentPickupDistance.get(p2vid(event.getPersonId()));
		double pickUpTime = this.currentPickupTime.get(p2vid(event.getPersonId()));
		double pickUpDuration = event.getTime() - pickUpTime;
		int timebin = getTimeBin(pickUpTime);
		this.currentOccupiedTripStartTime.put(p2vid(event.getPersonId()), event.getTime());
		this.pickupDistancePerHour[timebin] += pUdistance;
		this.pickupDurationPerHour[timebin] += pickUpDuration;

		String zone = getZoneForLinkId(event.getLinkId());
		if (zone != null) {
			double zonePickupdistance = this.zonePickUpDistance.get(zone) + pUdistance;
			double pickups = this.zoneRides.get(zone);
			pickups++;
			this.zoneRides.put(zone, pickups);
			this.zonePickUpDistance.put(zone, zonePickupdistance);

		}
		this.currentPickupDistance.put(p2vid(event.getPersonId()), 0.0);
	}

	private void handleStayEvent(ActivityEndEvent event) {
		vehicleOccupiedStatus.put(p2vid(event.getPersonId()), false);
		currentPickupTime.put(p2vid(event.getPersonId()), event.getTime());

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (vehicleOccupiedStatus.containsKey(event.getVehicleId())) {
			double length = network.getLinks().get(event.getLinkId()).getLength();

			if (vehicleOccupiedStatus.get(event.getVehicleId())) {
				Long rides = this.occupiedRides.get(event.getLinkId());
				rides++;
				this.occupiedRides.put(event.getLinkId(), rides);
				double distance = this.currentDropoffDistance.get(event.getVehicleId()) + length;
				this.currentDropoffDistance.put(event.getVehicleId(), distance);

			} else {
				Long rides = this.emptyRides.get(event.getLinkId());
				rides++;
				this.emptyRides.put(event.getLinkId(), rides);
				double distance = this.currentPickupDistance.get(event.getVehicleId()) + length;
				this.currentPickupDistance.put(event.getVehicleId(), distance);

			}
		}

	}

	private Id<Vehicle> p2vid(Id<Person> pid) {
		return Id.createVehicleId(pid.toString());
	}

	String getZoneForLinkId(Id<Link> linkId) {
		Coord linkCoord = network.getLinks().get(linkId).getCoord();
		Point linkPoint = MGC.coord2Point(linkCoord);
		for (Entry<String, Geometry> e : zones.entrySet()) {
			if (e.getValue().contains(linkPoint))
				return e.getKey();
		}

		return null;

	}

	public void evaluateAndWriteOutput(String outputDir) {
		evaluateZones();
		writeLinkStats(outputDir + "taxiLinkstats.csv");
		writeZoneStats(outputDir + "taxiZoneStats.csv");
		writeHourlyStats(outputDir + "taxiHourlyStats.csv");
	}

	private void writeHourlyStats(String filename) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat("####0.00");
		try {
			bw.write("timebin;rides;pickupDuration;pickupDistance;dropoffDuration;dropoffDistance");
			for (int i = 0; i < timebins; i++) {
				double rides = ridesPerHour[i];
				double pudu = pickupDurationPerHour[i] / rides;
				double pudi = pickupDistancePerHour[i] / rides;
				double drdu = dropoffDurationPerHour[i] / rides;
				double drdi = dropoffDistancePerHour[i] / rides;
				bw.newLine();
				bw.write(i + ";" + rides + ";" + df.format(pudu) + ";" + df.format(pudi) + ";" + df.format(drdu) + ";"
						+ df.format(drdi));
			}
			
			bw.flush();
			bw.close();
			double rides = new Sum().evaluate(ridesPerHour);
			double pudu = new Sum().evaluate(pickupDurationPerHour) / rides;
			double pudi = new Sum().evaluate(pickupDistancePerHour) / rides;
			double drdu = new Sum().evaluate(dropoffDurationPerHour) / rides;
			double drdi = new Sum().evaluate(dropoffDistancePerHour)/ rides;
			bw.newLine();
			bw.write("average;" + rides + ";" + df.format(pudu) + ";" + df.format(pudi) + ";" + df.format(drdu) + ";"
					+ df.format(drdi));

		} catch (IOException e) {
			// TODO: handle exception
		}
	}

	private void writeZoneStats(String filename) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat("####0.00");
		try {
			bw.write("Zone,EmptyRides,emptyKM,OccupiedRides,occupiedKM,Rides,PickUpDistance,AveragePickupDistance");
			for (Entry<String, double[]> e : zonesUsage.entrySet()) {
				double rides = this.zoneRides.get(e.getKey());
				double pickupDistance = this.zonePickUpDistance.get(e.getKey());
				double averagePickup = pickupDistance / rides;
				if (rides == 0.0)
					averagePickup = 0;
				bw.newLine();
				bw.write(e.getKey() + "," + df.format(e.getValue()[0]) + "," + df.format(e.getValue()[1]) + ","
						+ df.format(e.getValue()[2]) + "," + df.format(e.getValue()[3]) + "," + df.format(rides) + ","
						+ df.format(pickupDistance / 1000) + "," + df.format(averagePickup / 1000));

			}
			bw.flush();
			bw.close();

			BufferedWriter bw2 = IOUtils.getBufferedWriter(filename + "t");
			bw2.write("\"String\",\"Real\",\"Real\",\"Real\",\"Real\",\"Real\",\"Real\",\"Real\"");
			bw2.flush();
			bw2.close();
		} catch (IOException e) {
			// TODO: handle exception
		}
	}

	private void writeLinkStats(String filename) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		Locale.setDefault(Locale.US);
		DecimalFormat df = new DecimalFormat("####0.00");
		try {
			bw.write("Link;empty;occupied");
			for (Entry<Id<Link>, Long> e : this.emptyRides.entrySet()) {
				bw.newLine();
				bw.write(e.getKey() + ";" + e.getValue() + ";" + this.occupiedRides.get(e.getKey()));
			}
			bw.flush();
			bw.close();

			BufferedWriter bw2 = IOUtils.getBufferedWriter(filename + "t");
			bw2.write("\"String\",\"Real\",\"Real\"");
			bw2.flush();
			bw2.close();
		} catch (IOException e) {
			// TODO: handle exception
		}

	}

	private void evaluateZones() {
		for (Entry<Id<Link>, Long> entry : emptyRides.entrySet()) {
			Id<Link> linkId = entry.getKey();
			long empty = entry.getValue();
			long occupied = occupiedRides.get(linkId);
			double length = network.getLinks().get(linkId).getLength() / 1000;
			String zone = getZoneForLinkId(linkId);
			if (zone != null) {
				double[] zoneUsage = this.zonesUsage.get(zone);

				zoneUsage[0] += empty;
				zoneUsage[1] += (empty * length);
				zoneUsage[2] += occupied;
				zoneUsage[3] += (occupied * length);
				this.zonesUsage.put(zone, zoneUsage);
			}
		}
	}

	private int getTimeBin(double time) {
		return JbUtils.getHour(time);
	}

}
