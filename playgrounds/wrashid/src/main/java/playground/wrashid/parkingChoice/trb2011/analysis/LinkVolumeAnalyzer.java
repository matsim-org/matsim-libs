/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingChoice.trb2011.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity.PrivateParkingsIndoorWriter_v0;

public class LinkVolumeAnalyzer {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
//		String outputFolder="/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/runs/ktiRun24/output/";
		String outputFolder="H:/data/experiments/TRBAug2011/runs/ktiRun38/output/";
		final String networkFileName = outputFolder + "output_network.xml.gz";
		final String eventsFileName = outputFolder + "ITERS/it.50/50.events.xml.gz";
		final String plansFileName = outputFolder + "output_plans.xml.gz";
		final String facilitiesFileName = outputFolder + "output_facilities.xml.gz";

		EventsManager eventsManager = EventsUtils.createEventsManager();

		Network network = GeneralLib.readNetwork(networkFileName);

		VolumesAnalyzer volumeAnalyzer = new VolumesAnalyzer(3600, 24 * 3600 - 1, network);
		PeakHourAgents peakHourAgents = new PeakHourAgents(network);
		eventsManager.addHandler(volumeAnalyzer);
		eventsManager.addHandler(peakHourAgents);

		new MatsimEventsReader(eventsManager).readFile(eventsFileName);

		// peak defined as times, where there are more than 600'000 vehicles on
		// the road at the same time
		// for the given scenario. these are the intervalls between 7-9 and
		// 15-19.5

		PriorityQueue<SortableMapObject<Id>> priorityQueue = new PriorityQueue<SortableMapObject<Id>>();

		int count = 0;
		for (Id linkId : volumeAnalyzer.getLinkIds()) {
			double[] volumesPerHourForLink = volumeAnalyzer.getVolumesPerHourForLink(linkId);
			double volumeInPeakHours = getPeakHourVolums(volumesPerHourForLink);

			priorityQueue.add(new SortableMapObject<Id>(linkId, -volumeInPeakHours));
			count++;
		}

		// find out, which agents drive over the link during peak hours

		int selectTop10Percent = count / 10;
		LinkedList<Id> peakHourLinkIds = new LinkedList<Id>();

		for (int i = 0; i < selectTop10Percent; i++) {
			SortableMapObject<Id> sortableMapObject = priorityQueue.poll();
			peakHourLinkIds.add(sortableMapObject.getKey());
		}

		LinkedList<Id> peakHourFaciliyIds = new LinkedList<Id>();
		Scenario scenario = GeneralLib.readScenario(plansFileName, networkFileName, facilitiesFileName);
		ActivityFacilities facilities = scenario.getActivityFacilities();
		for (Id peakHourLinkId : peakHourLinkIds) {
			LinkedList<Id> agentIds = peakHourAgents.peakHourTravellingAgentLinkIds.get(peakHourLinkId);

			for (Id personId : agentIds) {
				Id facilityId = peakHourAgents.lastFacilityVisited.get(personId);
				if (facilityId != null && !peakHourFaciliyIds.contains(facilityId)) {
					ActivityFacility activityFacility = facilities.getFacilities().get(facilityId);
					if (GeneralLib.isInZHCityRectangle(activityFacility.getCoord())) {
						peakHourFaciliyIds.add(facilityId);
					}
					Id nextActivityFacilityId = getNextActivityFacilityId(facilityId,
							scenario.getPopulation().getPersons().get(personId).getSelectedPlan());
					activityFacility = facilities.getFacilities().get(facilityId);
					if (GeneralLib.isInZHCityRectangle(activityFacility.getCoord())) {
						peakHourFaciliyIds.add(nextActivityFacilityId);
					}
				}
			}

		}

		// find good clusters
		double clusterRadiusInMeters = 500;

		LinkedList<ActivityFacility> peakHourFacilities = new LinkedList<ActivityFacility>();

		for (Id activityFacilityId : peakHourFaciliyIds) {
			ActivityFacility activityFacility = facilities.getFacilities().get(activityFacilityId);
			peakHourFacilities.add(activityFacility);
		}

		QuadTree<ActivityFacilityImpl> facilitiesQuadTree = PrivateParkingsIndoorWriter_v0
				.getFacilitiesQuadTree(peakHourFacilities);

		PriorityQueue<SortableMapObject<ActivityFacility>> facilitiesPriorityQueue = new PriorityQueue<SortableMapObject<ActivityFacility>>();

		for (ActivityFacility actFacility : facilitiesQuadTree.values()) {
			// TODO: this could be improved, as the same facility could have
			// more than one act in it
			int numberOfActFacilitiesInEnvironment = facilitiesQuadTree.getDisk(actFacility.getCoord().getX(),
					actFacility.getCoord().getY(), clusterRadiusInMeters).size();
			facilitiesPriorityQueue.add(new SortableMapObject<ActivityFacility>(actFacility, -1.0
					* numberOfActFacilitiesInEnvironment));
		}

		// identify clusters (remove cluster duplicates/nearby clusters) =>
		// write on this also in the paper.
		HashMap<Coord, Double> clusterCenters = new HashMap<Coord, Double>();
		int leastNumberOfActsInCluster = 200;

		while (facilitiesPriorityQueue.size() > 0) {
			ActivityFacility actFacility = facilitiesPriorityQueue.poll().getKey();

			Coord clusterCenter = actFacility.getCoord();
			Collection<ActivityFacilityImpl> facilitiesInArea = facilitiesQuadTree.getDisk(actFacility.getCoord().getX(), actFacility
					.getCoord().getY(), clusterRadiusInMeters);
			double clusterScore = facilitiesInArea.size();
			if (clusterScore > leastNumberOfActsInCluster) {
				clusterCenters.put(clusterCenter, clusterScore);

				// remove all facilities of that area
				for (ActivityFacilityImpl facility : facilitiesInArea) {
					facilitiesQuadTree.remove(facility.getCoord().getX(), facility.getCoord().getY(), facility);
				}

			}
		}

		// visualize cluster centers
		String outputKmlFile = "H:/data/experiments/TRBAug2011/mainExperiment/kmls/clusterCenters.kml";
		BasicPointVisualizer basicPointVisualizer = new BasicPointVisualizer();

		System.out.println("Cluster Centers:");
		for (Coord coord : clusterCenters.keySet()) {
			System.out.println(coord.toString() + " -> " + clusterCenters.get(coord));
			basicPointVisualizer.addPointCoordinate(coord, clusterCenters.get(coord).toString(), Color.GREEN);
		}

		System.out.println("writing kml file...");
		basicPointVisualizer.write(outputKmlFile);

		// are there public garage parkings in that area?
		// LinkedList<Parking> parkingCollection=new LinkedList<Parking>();
		//
		// String streetParkingsFile=parkingDataBase + "streetParkings.xml";
		// readParkings(streetParkingCalibrationFactor,
		// streetParkingsFile,parkingCollection);
		//
		// String garageParkingsFile=parkingDataBase + "garageParkings.xml";
		// readParkings(garageParkingCalibrationFactor,
		// garageParkingsFile,parkingCollection);

		//

		// cluster them, e.g. cluster with high density, so that the method
		// works!
		// how much parking reduction needed?
	}

	public static double getPeakHourVolums(double[] volumesPerHourForLink) {
		double result = 0;
		// result += volumesPerHourForLink[7];
		// result += volumesPerHourForLink[8];
		result += volumesPerHourForLink[16];
		result += volumesPerHourForLink[17];
		result += volumesPerHourForLink[18];

		return result;
	}

	private static Id getNextActivityFacilityId(Id departureFacilityId, Plan plan) {
		int i = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity activity = (Activity) pe;
				if (((Leg) plan.getPlanElements().get(i + 1)).getMode().equalsIgnoreCase(TransportMode.car)) {
					return ((Activity) plan.getPlanElements().get(i + 2)).getFacilityId();
				}
			}
			i++;
		}

		DebugLib.stopSystemAndReportInconsistency("why was next act not found???");

		return null;
	}

	public static class PeakHourAgents implements LinkEnterEventHandler, ActivityStartEventHandler ,
	VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
		private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

		// linkId,personIds
		public LinkedListValueHashMap<Id, Id> peakHourTravellingAgentLinkIds = new LinkedListValueHashMap<Id, Id>();
		// persondId,facilityId
		public HashMap<Id, Id> lastFacilityVisited = new HashMap<Id, Id>();

		public HashMap<Id, Boolean> stopFollowingAgent = new HashMap<Id, Boolean>();
		private final Network network;

		public PeakHourAgents(Network network) {
			this.network = network;
		}

		@Override
		public void reset(int iteration) {
			delegate.reset(iteration);
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (GeneralLib.isInZHCityRectangle(network.getLinks().get(event.getLinkId()).getCoord())) {
				Id personId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
				if (!stopFollowingAgent.containsKey(personId) && ParkingManager.considerForParking(personId)) {
					if ((event.getTime() > 16 * 3600 && event.getTime() < 19 * 3600)) {
						this.peakHourTravellingAgentLinkIds.put(event.getLinkId(), personId);
						stopFollowingAgent.put(personId, null);
					}
				}
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			lastFacilityVisited.put(event.getPersonId(), event.getFacilityId());
		}

		public void handleEvent(VehicleEntersTrafficEvent event) {
			this.delegate.handleEvent(event);
		}

		public void handleEvent(VehicleLeavesTrafficEvent event) {
			this.delegate.handleEvent(event);
		}

	}

}
