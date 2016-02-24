/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.vsp.analysis.modules.simpleTripAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * A simple analysis-class for a very basic MATSim-Scenario, i.e it should be used 
 * with physical simulation of car-trips only. All other modes must be teleported. Thus,
 * this class will throw a runtime-exception when {@link ScenarioConfigGroup#isUseTransit()} is true. 
 * 
 * @author droeder
 *
 */
public final class SimpleTripAnalyzer extends AbstractPersonAlgorithm 
								implements LinkLeaveEventHandler,
										PersonArrivalEventHandler,
										PersonDepartureEventHandler, 
										PersonStuckEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(SimpleTripAnalyzer.class);
	private Map<Id<Person>, Traveller> traveller;
	private Network net;
	private Set<Id<Person>> pIds;
	private Map<String, Double> distFactors;
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
	
	public  SimpleTripAnalyzer(Config c, Network net, Set<Id<Person>> pIds) throws RuntimeException{
		if(c.transit().isUseTransit() ){
			throw new RuntimeException("This analysis is structured very simple. " +
					"Thus, it does not allow physically simulated transit!");
		}
		this.distFactors = c.plansCalcRoute().getBeelineDistanceFactors() ;
		this.net = net;
		this.pIds = pIds;
	}

	@Override
	public void reset(int iteration) {
		traveller = new HashMap<>();
		delegate.reset(iteration);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if( (pIds != null) && !pIds.contains(event.getPersonId())) return;
		Traveller t = this.traveller.get(event.getPersonId());
		if(t == null){
			t = new Traveller(event.getPersonId());
			this.traveller.put(event.getPersonId(), t);
		}
		String mode = event.getLegMode();
		// a single transitWalk should be handled not as pt bus as walk...
		mode = (mode == TransportMode.transit_walk) ? TransportMode.walk : mode;
		// cars start at the end of a link, thus we subtract the link-length for car-trips.
		//the distance for other modes is Double.NaN as they are teleported.
		t.startTrip(event.getTime(), mode, 
				(mode == TransportMode.car) ? (-calcLinkDistance(event.getLinkId())) : Double.NaN);
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Traveller t = this.traveller.get(delegate.getDriverOfVehicle(event.getVehicleId()));
		if(t == null){
			return;
		}
		t.passLink(calcLinkDistance(event.getLinkId()));
	}

	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Traveller t = this.traveller.get(event.getPersonId());
		if(t == null){
			return;
		}
		t.endTrip(event.getTime(), calcLinkDistance(event.getLinkId()));
	}
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		Traveller t = this.traveller.get(event.getPersonId());
		if(t == null){
			return;
		}
		t.setStuck();
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void run(Person person) {
		Traveller t = this.traveller.get(person.getId());
		if(t == null){
			return;
		}
		List<PlanElement> pe = person.getSelectedPlan().getPlanElements();
		t.maxTripIndex = (pe.size()/2) - 1;
//		System.out.println(pe.size());
//		System.out.println(t.toString());
		for(int i = 2; i < pe.size(); i += 2){
			Trip trip = t.getTrips().get((i/2)-1);
			Activity start = (Activity) pe.get(i-2);
			Activity end = (Activity) pe.get(i);
			trip.from = start.getCoord();
			trip.to = end.getCoord();
			// the beeline is always the same
			trip.beeline = calcBeelineDistance(trip.from, trip.to);
			// dist for non-car-modes is calculated with a factor
			if(!trip.mode.equals(TransportMode.car)){
				trip.dist = this.distFactors.get( trip.mode ) * trip.beeline; 
			}
			// there will be no more trips
			if(trip.stuck) return;
		}
	}

//	##################helper methods #######################
	
	/**
	 * @param linkId
	 * @return
	 */
	private double calcLinkDistance(Id<Link> linkId) {
		return this.net.getLinks().get(linkId).getLength();
	}

	/**
	 * @param from
	 * @param to
	 * @return
	 */
	private Double calcBeelineDistance(Coord from, Coord to) {
		if(from == null || to == null) return Double.NaN;
		return CoordUtils.calcEuclideanDistance(from, to);
	}

	/**
	 * @param outputPath
	 */
	public void dumpData(String outputPath, String fileprefix) {
		BufferedWriter w = IOUtils.getBufferedWriter(outputPath + System.getProperty("file.separator") + 
				((fileprefix == null) ? "" : (fileprefix.endsWith(".") ? fileprefix : (fileprefix + "."))) + "trips.csv.gz");
		try {
			w.write(Traveller.HEADER + "\n");
			for(Traveller t : traveller.values()){
				w.write(t.toString());
			}
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

//##################getter/setter###############################
	
	public final Map<Id<Person>, Traveller> getTraveller(){
		return traveller;
	}
	
}

