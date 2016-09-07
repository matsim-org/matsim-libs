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

package playground.jbischoff.taxibus.scenario.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class WhoIsInBoxPlotter {
	public static void main(String[] args) throws IOException {
		String eventsFile = "D:/runs-svn/braunschweig/output/bs05/output_events.xml.gz";
		String networkF = "D:/runs-svn/vw_rufbus/VW79BC/VW79BC.output_network.xml.gz";
		String shapeFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/vehonroad/quader.shp";
		String outputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/vehonroad/vehiclesInBox_gliesmarode.csv";
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkF);
		Geometry geo = JbUtils.readShapeFileAndExtractGeometry(shapeFile, "id").get("3");
		List<Id<Link>> relevantLinkIds = extractRelevantLinks(network,geo);
		EventsManager events = EventsUtils.createEventsManager();
		BoxHandler boxHandler = new BoxHandler(relevantLinkIds);
		events.addHandler(boxHandler);
		new MatsimEventsReader(events).readFile(eventsFile);
		writeArray(boxHandler.getVehiclesInBox(),outputFile);
		
	}

	private static void writeArray(int[] vehiclesInBox, String outputFile) throws IOException {
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		bw.write("time;vehicles");
		for (int i = 0;i<vehiclesInBox.length;i++){
			bw.newLine();
			bw.write(Time.writeTime(i)+";"+vehiclesInBox[i]);
		}
		bw.flush();
		bw.close();
	}

	private static List<Id<Link>> extractRelevantLinks(Network network, Geometry geo) {
		List<Id<Link>> relevantLinks = new ArrayList<>();
		for (Link l : network.getLinks().values()){
			if ((l.getAllowedModes().size() == 1)&&l.getAllowedModes().contains("pt")) continue;
			if (geo.contains(MGC.coord2Point(l.getCoord()))){
				relevantLinks.add(l.getId());
			}
		}
		System.out.println("links "+relevantLinks.size());
		return relevantLinks;
	}
}

class BoxHandler implements LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler{

	Map<Id<Vehicle>,Double> enterTimes = new HashMap<>();
	final List<Id<Link>> relevantLinks;
	int[] vehiclesInBox = new int[30*3600];
	
	public BoxHandler(List<Id<Link>> relevantLinks) {
		this.relevantLinks = relevantLinks;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Vehicle> vid = event.getVehicleId();
		
		if (relevantLinks.contains(event.getLinkId())){
			if (!enterTimes.containsKey(vid)){
				enterTimes.put(vid, event.getTime());
			}
		}
		else {
			if (enterTimes.containsKey(vid)){
				double enterTime = enterTimes.remove(vid);
				addVehicle (enterTime,event.getTime());
			}
		}
		
	}
	private void addVehicle(double enterTime, double leaveTime){
		if ((enterTime>vehiclesInBox.length)||(leaveTime>vehiclesInBox.length)) return;
		
		for (int i = (int) enterTime; i<= leaveTime; i++){
			this.vehiclesInBox[i]++;
		}
	}
	public int[] getVehiclesInBox() {
		return vehiclesInBox;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());
		if (enterTimes.containsKey(vid)){
			double enterTime = enterTimes.remove(vid);
			addVehicle (enterTime,event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if ((relevantLinks.contains(event.getLinkId()))&&event.getLegMode().equals(TransportMode.car)){
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());
			enterTimes.put(vid, event.getTime());

		}		
	}
	}
