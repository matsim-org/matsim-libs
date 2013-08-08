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

package playground.vsp.analysis.modules.ptLines2PaxAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
 * @author sfuerbas (parts taken from droeder)
 *
 */

public class TransitLines2PaxCounts {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitLines2PaxCounts.class);
	private Double interval;
	private Id id;
	private Integer maxSlice;
	private Counts boarding;
	private Counts alighting;
	private Counts capacity;
	private Counts totalPax;
	private Counts occupancy;
	private List<TransitRouteStop> longestRoute_a;
	private List<TransitRouteStop> longestRoute_b;
	private List<TransitRoute> routeList;
	
	public TransitLines2PaxCounts (TransitLine tl, double countsInterval, int maxSlice) {
		this.id = tl.getId();
		this.boarding = new Counts();
		this.alighting = new Counts();
		this.capacity = new Counts();
		this.totalPax = new Counts();
		this.occupancy = new Counts();
		this.interval = countsInterval;
		this.maxSlice = maxSlice;
		this.routeList = new ArrayList<TransitRoute>();
		for (TransitRoute tr : tl.getRoutes().values()) {
			this.routeList.add(tr);
			int numberOfStops = tr.getStops().size();
			for (int ii=0; ii < numberOfStops; ii++) {
				TransitRouteStop s = tr.getStops().get(ii);
				Id stopFacilId = s.getStopFacility().getId();
				if (this.boarding.getCounts().get(stopFacilId)==null) {
					this.boarding.createCount(stopFacilId, stopFacilId.toString());
					this.alighting.createCount(stopFacilId, stopFacilId.toString());
					this.capacity.createCount(stopFacilId, stopFacilId.toString());
					this.totalPax.createCount(stopFacilId, stopFacilId.toString());
					this.occupancy.createCount(stopFacilId, stopFacilId.toString());
				}
			}
		}	
//		remove duplicate routes
		cleanRouteList();
	}
	
	
	public List<TransitRoute> getRoutesByNumberOfStops() {
		Collections.sort(this.routeList, new RouteSizeComparator());
		return this.routeList;
	}

	
//	this might not be the best way of removing duplicate routes, it seems to work properly nontheless
	private void cleanRouteList () {
		Map<Integer, List<TransitRoute>> stops2routes = new HashMap<Integer, List<TransitRoute>>();
		List<TransitRoute> dupeRoutes = new ArrayList<TransitRoute>();
		int numberOfRoutes = this.routeList.size();
//		put all TransitRoutes in map with number of stops as key 
		for (int ii=0; ii<numberOfRoutes; ii++) {
			TransitRoute tr = this.routeList.get(ii);
			Integer numberOfStops = tr.getStops().size();
			if (stops2routes.get(numberOfStops) == null) {
				stops2routes.put(numberOfStops, new ArrayList<TransitRoute>());
				stops2routes.get(numberOfStops).add(tr);
			}
			else {
				stops2routes.get(numberOfStops).add(tr);
			}
		}
		for (Integer ns: stops2routes.keySet()) {
			for (int jj=this.routeList.size(); jj>1; jj--) {
				TransitRoute tr = this.routeList.get(jj-1);
				for (int kk=0; kk<stops2routes.get(ns).size(); kk++) {
					TransitRoute tr2 = stops2routes.get(ns).get(kk);
					if ((tr.getId()!=tr2.getId()) && (tr2.getStops().containsAll(tr.getStops()))) { 
						dupeRoutes.add(tr);
					}
				}
			}
		}
		log.warn("NUMBER OF ROUTES ON DUPE LIST: "+dupeRoutes.size()+" WITH TOTAL ROUTES: "+this.routeList.size());
		this.routeList.removeAll(dupeRoutes);
	}

	
	private void createRouteSegments () {
		Map<Integer, List<TransitRoute>> stops2routes = new HashMap<Integer, List<TransitRoute>>();
		List<TransitRoute> dupeRoutes = new ArrayList<TransitRoute>();
		List<TransitRoute> routeFragments = new ArrayList<TransitRoute>(); // Liste der Routenfragmente
		int numberOfRoutes = this.routeList.size();
//		put all TransitRoutes in map with number of stops as key 
		for (int ii=0; ii<numberOfRoutes; ii++) {
			TransitRoute tr = this.routeList.get(ii);
			Integer numberOfStops = tr.getStops().size();
			if (stops2routes.get(numberOfStops) == null) {
				stops2routes.put(numberOfStops, new ArrayList<TransitRoute>());
				stops2routes.get(numberOfStops).add(tr);
			}
			else {
				stops2routes.get(numberOfStops).add(tr);
			}
		}
		
		for (Integer ns: stops2routes.keySet()) {
			for (int jj=this.routeList.size(); jj>1; jj--) {
				TransitRoute tr = this.routeList.get(jj-1);
				int trLength = tr.getStops().size();
				for (int kk=0; kk < trLength; kk++) {
					
					
					TransitRoute tr2 = stops2routes.get(ns).get(kk);
					int tr2Length = tr2.getStops().size();
					
					if ((tr.getId()!=tr2.getId())) {
						
						boolean createFragment = false;
						
						//Unterscheidung nötig: Routensegment am Anfang, Routensegment am Ende, Routensegment in der Mitte (=Hauptroute?)
						
						//erster Stop identisch --> vorwärts durchlaufen ab index 0
						if (tr.getStops().get(0) == tr2.getStops().get(0)) {
							
							//wenn TR2 zu Ende und Stops mit TR identisch --> Route entfernen

							for (int rr1=0; rr1 < tr2Length; ) {
								if (tr.getStops().get(rr1) == tr2.getStops().get(rr1)) rr1++; //nichts tun, nur rr1 erhöhen
								else if ((tr.getStops().get(rr1) != tr2.getStops().get(rr1)) && !createFragment) {
							//Fragment erstellen sobald erster Stop nicht mehr in beiden Routen enthalen
									createFragment = true;
									//hier Fragment erstellen und ersten Stop hinzufügen
									rr1++;
								}
								else if (createFragment) {
									//hier weitere Stops zum Fragment hinzufügen
									dupeRoutes.add(tr2);
									rr1++;
								}
								else if (!createFragment && (rr1 == tr2Length-1)) {
									//wenn bis Erreichen der Länge von tr2 alle Stops gleich sind --> rauswerfen
									dupeRoutes.add(tr2);
									rr1++;
								}
							}
														
						}
						
						//letzter Stop identisch --> vorwärts durchlaufen ab index von (erster Stop beider Routen)
						else if (tr.getStops().get(trLength-1) == tr2.getStops().get(tr2Length-1)) {
							
							//Fragment erstellen ab TR.stopIndex(0) bis erstem identischen Stop
							
							//prüfen ob Rest identisch
								//ja --> Route entfernen
								//nein --> neues Fragment
							
						}
						
						//weder erster noch letzter identisch
						else {
							//TR durchlaufen und prüfen ob stop(index) in TR2 enthalten
							
							//wenn enthalten: vorwärts durchlaufen ab index von (erster Stop beider Routen) bis index (letzter Stop beider Routen)
							//kein Fragment erstellen, wenn alle Stops in TR2 auch in TR enthalten sind und Reihenfolge stimmt
						}
						
					}
				
				}
			}
		}
		
	}
	
	public class RouteSizeComparator implements Comparator<TransitRoute> {
		public int compare(TransitRoute route1, TransitRoute route2) {
			Integer stopSize1 = route1.getStops().size();
			Integer stopSize2 = route2.getStops().size();
			if (stopSize1 > stopSize2) return 1;
			else if (stopSize1 > stopSize2) return -1;
			else return 0;
		}
	}
	
	@SuppressWarnings("unused")
	private void findLongestRoute (List<TransitRouteStop> route_c) {
		int length_a;
		int length_b;
		int length_c = route_c.size();
		if (this.longestRoute_a == null && this.longestRoute_b == null) {
			this.longestRoute_a = route_c;
		}
		else if (this.longestRoute_a != null && this.longestRoute_b == null) {
			length_a = this.longestRoute_a.size();
			if (length_c > length_a && route_c.containsAll(this.longestRoute_a)) {
				this.longestRoute_a = route_c;
			}
			else if (!this.longestRoute_a.containsAll(route_c) && !route_c.containsAll(this.longestRoute_a)) {
				this.longestRoute_b = route_c;
			}
		}
		else if (this.longestRoute_a != null && this.longestRoute_b != null) {
			length_a = this.longestRoute_a.size();
			length_b = this.longestRoute_b.size();
			if (length_c > length_a && route_c.containsAll(this.longestRoute_a)) {
				this.longestRoute_a = route_c;
			}
			else if (length_c > length_b && route_c.containsAll(longestRoute_b)) {
				this.longestRoute_b = route_c;
			}
		}
	}
	
	
	public List<TransitRouteStop> getLongestRoute_a() {
		return longestRoute_a;
	}

	public List<TransitRouteStop> getLongestRoute_b() {
		return longestRoute_b;
	}

	public Id getId(){
		return this.id;
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxBoarding(Id facilityId, double time) {
		increase(this.boarding, facilityId, time, 1.);
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxAlighting(Id facilityId, double time) {
		increase(this.alighting, facilityId, time, 1.);
	}

	/**
	 * @param time
	 * @param vehCapacity
	 * @param nrSeatsInUse
	 * @param stopIndexId
	 */
	public void vehicleDeparts(double time, double vehCapacity,	double nrSeatsInUse, Id stopFacilityId) {
		if(this.alighting.getCount(stopFacilityId).getVolume(getTimeSlice(time)) == null){
			set(this.alighting, stopFacilityId, time, 0);
		}
		if(this.boarding.getCount(stopFacilityId).getVolume(getTimeSlice(time)) == null){
			set(this.boarding, stopFacilityId, time, 0);
		}
		increase(this.capacity, stopFacilityId, time, vehCapacity);
		increase(this.totalPax, stopFacilityId, time, nrSeatsInUse);
		Integer slice = getTimeSlice(time);
		set(this.occupancy, stopFacilityId, time, this.totalPax.getCount(stopFacilityId).getVolume(slice).getValue() /
				this.capacity.getCount(stopFacilityId).getVolume(slice).getValue());
	}
	
	private void increase(Counts counts, Id stopFacilityId, Double time, double increaseBy){
		Count count = counts.getCount(stopFacilityId);
		Integer slice = getTimeSlice(time);
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + increaseBy);
	}
	
	private void set(Counts counts, Id stopFacilityId, Double time, double value){
		Count count =  counts.getCount(stopFacilityId);
		Integer slice = getTimeSlice(time);
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(value);
	}
	
	private Integer getTimeSlice(double time){
		int slice =  (int) (time / this.interval);
		if(slice >= this.maxSlice){
			return this.maxSlice;
		}
		return slice;
	}

	/**
	 * @return the boarding
	 */
	public Counts getBoarding() {
		return boarding;
	}

	/**
	 * @return the alighting
	 */
	public Counts getAlighting() {
		return alighting;
	}

	/**
	 * @return the capacity
	 */
	public Counts getCapacity() {
		return capacity;
	}

	/**
	 * @return the totalPax
	 */
	public Counts getTotalPax() {
		return totalPax;
	}

	/**
	 * @return the occupancy
	 */
	public Counts getOccupancy() {
		return occupancy;
	}
	
	public Integer getMaxSlice(){
		return this.maxSlice;
	}

}
