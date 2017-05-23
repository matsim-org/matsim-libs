/* *********************************************************************** *
 * project: org.matsim.*
 * RouteTTObserver
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsptelematics.ha2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.math.stat.StatUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import com.google.inject.Inject;

import playground.vsptelematics.common.ListUtils;

@Singleton
public class GuidanceRouteTTObserver implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		LinkEnterEventHandler, IterationEndsListener, AfterMobsimListener, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private Set<Id<Person>> route1;
	private Set<Id<Person>> route2;
	private Map<Id<Person>, Double> personTTs;
	private Map<Id<Person>, Double> departureTimes;

	private BufferedWriter writer;

	public double avr_route1TTs;
	public double avr_route2TTs;
	private double avgGuidedTTs;
	private double avgUnGuidedTTs;
	private double sumRoute1TTs;
	private double sumRoute2TTs;

	private String filename;

	private Set<Id<Person>> guidedAgentIds;
	private Set<Id<Person>> unGuidedAgentIds;
	private HashMap<Id<Person>, Double> unGuidedPersonTTs;
	private HashMap<Id<Person>, Double> guidedPersonTTs;
	
	Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();

	@Inject
	public GuidanceRouteTTObserver(OutputDirectoryHierarchy controlerIO) {
		this.filename = controlerIO.getOutputFilename("routeTravelTimes.txt");
		this.reset(0);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		departureTimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void reset(int iteration) {
		route1 = new HashSet<>();
		route2 = new HashSet<>();
		personTTs = new HashMap<>();
		unGuidedPersonTTs = new HashMap<>();
		guidedPersonTTs = new HashMap<>();
		departureTimes = new HashMap<>();
		unGuidedAgentIds = new HashSet<>();
		guidedAgentIds = new HashSet<>();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double depTime = departureTimes.get(event.getPersonId());
		if (depTime == 0)
			throw new RuntimeException("Agent departure time not found!");

		double tt = event.getTime() - depTime;
		personTTs.put(event.getPersonId(), tt);
		if (this.guidedAgentIds.contains(event.getPersonId())){
			this.guidedPersonTTs.put(event.getPersonId(), tt);
		}
		else {
			this.unGuidedPersonTTs.put(event.getPersonId(), tt);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().toString().equals("4")) {
			route1.add(vehicle2driver.getDriverOfVehicle(event.getVehicleId()));
		}
		else if (event.getLinkId().toString().equals("5")) {
			route2.add(vehicle2driver.getDriverOfVehicle(event.getVehicleId()));
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			writer = org.matsim.core.utils.io.IOUtils.getBufferedWriter(filename);
			writer.write("it\tn_1\tn_2\ttt_avg_1\ttt_avg_2\ttt_sum_1\ttt_sum_2\ttt_sum\tavg_guided_tt\tavg_unguided_tt");
			writer.newLine();
			writer.write(String.valueOf(event.getIteration()));
			writer.write("\t");
			writer.write(String.valueOf(route1.size()));
			writer.write("\t");
			writer.write(String.valueOf(route2.size()));
			writer.write("\t");

			if (route1.isEmpty())
				writer.write("0");
			else
				writer.write(String.valueOf(avr_route1TTs));
			writer.write("\t");

			if (route2.isEmpty())
				writer.write("0");
			else
				writer.write(String.valueOf(avr_route2TTs));

			writer.write("\t");
			writer.write(String.valueOf(sumRoute1TTs));
			writer.write("\t");
			writer.write(String.valueOf(sumRoute2TTs));
			writer.write("\t");
			writer.write(String.valueOf(sumRoute1TTs + sumRoute2TTs));
			
			writer.write("\t");
			writer.write(String.valueOf(this.avgGuidedTTs));
			writer.write("\t");
			writer.write(String.valueOf(this.avgUnGuidedTTs));
			
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		List<Double> route1TTs = new ArrayList<>();
		List<Double> route2TTs = new ArrayList<>();

		for (Id<Person> p : route1) {
			route1TTs.add(personTTs.get(p));
		}
		for (Id<Person> p : route2) {
			route2TTs.add(personTTs.get(p));
		}

		sumRoute1TTs = StatUtils.sum(ListUtils.toArray(route1TTs));
		sumRoute2TTs = StatUtils.sum(ListUtils.toArray(route2TTs));

		avr_route1TTs = StatUtils.mean(ListUtils.toArray(route1TTs));
		avr_route2TTs = StatUtils.mean(ListUtils.toArray(route2TTs));

		if (Double.isNaN(avr_route1TTs)) {
            avr_route1TTs = getFreespeedTravelTime(event.getServices().getScenario().getNetwork().getLinks()
					.get(Id.create("2", Link.class)));
            avr_route1TTs += getFreespeedTravelTime(event.getServices().getScenario().getNetwork().getLinks()
					.get(Id.create("4", Link.class)));
            avr_route1TTs += getFreespeedTravelTime(event.getServices().getScenario().getNetwork().getLinks()
					.get(Id.create("6", Link.class)));
		}
		if (Double.isNaN(avr_route2TTs)) {
            avr_route2TTs = getFreespeedTravelTime(event.getServices().getScenario().getNetwork().getLinks()
					.get(Id.create("3", Link.class)));
            avr_route2TTs += getFreespeedTravelTime(event.getServices().getScenario().getNetwork().getLinks()
					.get(Id.create("5", Link.class)));
            avr_route2TTs += getFreespeedTravelTime(event.getServices().getScenario().getNetwork().getLinks()
					.get(Id.create("6", Link.class)));
		}
	
		this.avgGuidedTTs = 0.0;
		if (! this.guidedPersonTTs.isEmpty()){
			for (Double t : this.guidedPersonTTs.values()) {
				this.avgGuidedTTs += t;
			}
			this.avgGuidedTTs /= this.guidedPersonTTs.size();
		}
	
		
		this.avgUnGuidedTTs = 0.0;
		if (! this.unGuidedPersonTTs.isEmpty()){
			for (Double t : this.unGuidedPersonTTs.values()) {
				this.avgUnGuidedTTs += t;
			}
			this.avgUnGuidedTTs /= this.unGuidedPersonTTs.size();
		}
		
	}

	private double getFreespeedTravelTime(final Link link) {
		return link.getLength() / link.getFreespeed();
	}

	public void addUnGuidedAgentId(Id<Person> id) {
		this.unGuidedAgentIds.add(id);
	}

	public void addGuidedAgentId(Id<Person> id) {
		this.guidedAgentIds.add(id);
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}


	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		vehicle2driver.handleEvent(event);
	}

}