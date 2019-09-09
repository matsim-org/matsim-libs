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
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author sfuerbas (parts taken from droeder)
 *
 */

public class TransitLines2PaxCounts {
	
	private static final Logger log = Logger
			.getLogger(TransitLines2PaxCounts.class);
	private Double interval;
	private Id<TransitLine> id;
	private Integer maxSlice;
	private Counts boarding;
	private Counts alighting;
	private Counts capacity;
	private Counts totalPax;
	private Counts occupancy;
	private List<TransitRoute> routeList;
	private TransitScheduleFactory tsf;
	
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
		this.tsf = new TransitScheduleFactoryImpl();
		for (TransitRoute tr : tl.getRoutes().values()) {
			this.routeList.add(tr);
			int numberOfStops = tr.getStops().size();
			for (int ii=0; ii < numberOfStops; ii++) {
				TransitRouteStop s = tr.getStops().get(ii);
				Id<TransitStopFacility> stopFacilId = s.getStopFacility().getId();
				if (this.boarding.getCounts().get(stopFacilId)==null) {
					Id<Link> convertedId = Id.create(stopFacilId, Link.class);
					this.boarding.createAndAddCount(convertedId, stopFacilId.toString());
					this.alighting.createAndAddCount(convertedId, stopFacilId.toString());
					this.capacity.createAndAddCount(convertedId, stopFacilId.toString());
					this.totalPax.createAndAddCount(convertedId, stopFacilId.toString());
					this.occupancy.createAndAddCount(convertedId, stopFacilId.toString());
				}
			}
		}	
		sortRoutesByNumberOfStops();
		createRouteSegments();
		// execute twice to ensure all added fragment duplicates are detected and removed
		sortRoutesByNumberOfStops();
		createRouteSegments();
	}
	
	
	public List<TransitRoute> sortRoutesByNumberOfStops() {
		Collections.sort(this.routeList, new RouteSizeComparator());
		return this.routeList;
	}

	public class RouteSizeComparator implements Comparator<TransitRoute> {
		@Override
		public int compare(TransitRoute route1, TransitRoute route2) {
			Integer stopSize1 = route1.getStops().size();
			Integer stopSize2 = route2.getStops().size();
			return stopSize2.compareTo(stopSize1);
		}
	}
	
	public List<TransitRoute> getRouteList() {
		return this.routeList;
	}

	
	private void createRouteSegments () {
		List<Id> comparedRoutes = new ArrayList<>();
		List<TransitRoute> noFittingSchemeRoutes = new ArrayList<TransitRoute>();
		List<TransitRoute> dupeRoutes = new ArrayList<TransitRoute>();
		List<TransitRoute> routeFragments = new ArrayList<TransitRoute>(); // Liste der Routenfragmente
		int numberOfRoutes = this.routeList.size();
		
		for (int ii = 0; ii < numberOfRoutes; ii++) {
				// Routen tr von lang nach kurz durchlaufen: Diese wird mit allen anderen verglichen
				TransitRoute tr = this.routeList.get(ii);
				int trLength = tr.getStops().size();
						
				//Für tr alle anderen Routen durchlaufen, andere Route = tr2, von kurz nach lang
				for (int jj=numberOfRoutes-1; jj >= 0; jj--) {
					TransitRoute tr2 = this.routeList.get(jj);
					int tr2Length = tr2.getStops().size();
					
					//nur vergleichen, wenn wirklich unterschiedliche Routen vorliegen,
					//tr noch nicht abgearbeitet ist und tr2 noch nicht als Duplikat einer anderen Route erkannt wurde
					if (!comparedRoutes.contains(tr2.getId()) && !dupeRoutes.contains(tr2) && !tr.getId().equals(tr2.getId())) {
						if (tr2Length > trLength) {
							log.error("TransitRoute tr2 to be compared is longer than base TransitRoute tr, this should not happen. \n " +
									"tr ID: "+tr.getId()+" with length: "+trLength+" and tr2 ID: "+tr2.getId()+" with length: "+tr2Length);
							continue;
						}
						boolean createFragment = false;
						List<TransitRouteStop> routeFragmentStopList = new ArrayList<TransitRouteStop>();
						//Unterscheidung nötig: Routensegment am Anfang, Routensegment am Ende, Routensegment in der Mitte
						
						//erster Stop identisch
						if (tr.getStops().get(0).getStopFacility().getId().equals(tr2.getStops().get(0).getStopFacility().getId())) {
							// tr2 durchlaufen von Anfang bis Ende
							for (int rr=0; rr < tr2Length; rr++) {
								// so lange tr2 stop = tr stop nur rr erhöhen
								if (tr.getStops().get(rr).getStopFacility().getId().equals(tr2.getStops().get(rr).getStopFacility().getId())) {
									 // bei Erreichen des letzten Stop von tr2: tr2 als Duplikat erkannt
									if (rr == tr2Length-1) {
										if (!dupeRoutes.contains(tr2)) dupeRoutes.add(tr2);
									}
								}
								// 
								else if (createFragment) {
									// weitere Stops zum Fragment hinzufügen
									routeFragmentStopList.add(tr2.getStops().get(rr));
									//wenn letzter Stop von tr2 erreicht: Fragment-Route aus Stop-Liste erstellen
									if (rr == tr2Length-1) {
										Id<TransitRoute> fragmentId = Id.create(tr2.getId().toString()+"_"+routeFragments.size(), TransitRoute.class);
										TransitRoute frag = this.tsf.createTransitRoute(fragmentId, tr2.getRoute(), routeFragmentStopList, tr2.getTransportMode());
										routeFragments.add(frag);
										if (!dupeRoutes.contains(tr2)) {
											dupeRoutes.add(tr2);
										}
									}
								}
								else if (!(tr.getStops().get(rr).getStopFacility().getId().equals(tr2.getStops().get(rr).getStopFacility().getId())) && !createFragment) {
							//Fragment erstellen sobald erster Stop nicht mehr in beiden Routen enthalen
									createFragment = true;
									//hier Fragment erstellen und vorherigen und aktuellen Stop hinzufügen
									routeFragmentStopList.add(tr2.getStops().get(rr-1));
									routeFragmentStopList.add(tr2.getStops().get(rr));
								}
							}
						}
						
						//letzter Stop identisch
						else if (tr.getStops().get(trLength-1).getStopFacility().getId().equals(tr2.getStops().get(tr2Length-1).getStopFacility().getId()) ) {
							//siehe oben, rückwärts
							for (int rr=tr2Length-1; rr >= 0; rr--) {
								if (!createFragment && tr.getStops().get(trLength-1-(tr2Length-1-rr)).getStopFacility().getId().equals(tr2.getStops().get(rr).getStopFacility().getId())) {
									 //nichts tun, nur rr verringern
									if (rr == 0) {
										if (!dupeRoutes.contains(tr2)) dupeRoutes.add(tr2);
									}
								}
								else if (createFragment) {
									//hier weitere Stops zum Fragment hinzufügen
									if (!dupeRoutes.contains(tr2)) {
										dupeRoutes.add(tr2);
									}
									routeFragmentStopList.add(tr2.getStops().get(rr));
									//wenn letzter Stop von tr2 erreicht: Fragment-Route aus Stop-Liste erstellen
									if (rr == 0) {
										Collections.reverse(routeFragmentStopList);
										Id<TransitRoute> fragmentId = Id.create(tr2.getId().toString()+"_"+routeFragments.size(), TransitRoute.class);
										TransitRoute frag = this.tsf.createTransitRoute(fragmentId, tr2.getRoute(), routeFragmentStopList, tr2.getTransportMode());
										routeFragments.add(frag);
									}
								}
								else if (!createFragment && (rr == 0)) {
									//wenn bis Erreichen der Länge von tr2 alle Stops gleich sind --> rauswerfen
									if (!dupeRoutes.contains(tr2)) dupeRoutes.add(tr2);
									
								}
								else if (!(tr.getStops().get(trLength-1-(tr2Length-1-rr)).getStopFacility().getId().equals(tr2.getStops().get(rr).getStopFacility().getId())) && !createFragment) {
							//Fragment erstellen sobald erster Stop nicht mehr in beiden Routen enthalen
									createFragment = true;
									//hier Fragment erstellen und ersten Stop hinzufügen
									routeFragmentStopList.add(tr2.getStops().get(rr));
								}
							}
						}
						
						//weder erster noch letzter identisch
						else {
							//TR durchlaufen und prüfen ob stop(index) als erster Stop in TR2 enthalten
							for (int rr = 0; rr < trLength; rr++) {
								if (tr2.getStops().get(0).getStopFacility().getId().equals(tr.getStops().get(rr).getStopFacility().getId())) {
									for (int ss = 0; ss < tr2Length; ss++) {
										if ((rr+ss) <= trLength-1) {
											if (tr2.getStops().get(ss).getStopFacility().getId().equals(tr.getStops().get(rr+ss).getStopFacility().getId())) {
												if (ss == tr2Length-1) {
													if (!dupeRoutes.contains(tr2)) dupeRoutes.add(tr2);
												}
											}
										}
										else {
											log.error("Fragment may need to be created, check output manually for now.");
										}
									}
								}
							}
							log.info("Does not fit into current route scheme: "+"tr ID: "+tr.getId()+" with length: "+trLength+" and tr2 ID: "+tr2.getId()+" with length: "+tr2Length);
							if (!noFittingSchemeRoutes.contains(tr2)) noFittingSchemeRoutes.add(tr2);
						}
					}

				}
				comparedRoutes.add(tr.getId());
			}
		noFittingSchemeRoutes.removeAll(dupeRoutes);
		log.error("No fitting route schemes were found for routes: "+noFittingSchemeRoutes);
		this.routeList.removeAll(dupeRoutes);
		this.routeList.addAll(routeFragments);
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
					if ((!dupeRoutes.contains(tr)) && (tr.getId()!=tr2.getId()) && (tr2.getStops().containsAll(tr.getStops()))) { 
						dupeRoutes.add(tr);
					}
				}
			}
		}
		log.warn("NUMBER OF ROUTES ON DUPE LIST: "+dupeRoutes.size()+" WITH TOTAL ROUTES: "+this.routeList.size());
		this.routeList.removeAll(dupeRoutes);
	}
	

	public Id<TransitLine> getId(){
		return this.id;
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxBoarding(Id<TransitStopFacility> facilityId, double time) {
		increase(this.boarding, facilityId, time, 1.);
	}

	/**
	 * @param facilityId
	 * @param time
	 */
	public void paxAlighting(Id<TransitStopFacility> facilityId, double time) {
		increase(this.alighting, facilityId, time, 1.);
	}

	/**
	 * @param time
	 * @param vehCapacity
	 * @param nrSeatsInUse
	 * @param stopIndexId
	 */
	public void vehicleDeparts(double time, double vehCapacity,	double nrSeatsInUse, Id<TransitStopFacility> stopFacilityId) {
		Id<Link> convertedId = Id.create(stopFacilityId, Link.class);
		if(this.alighting.getCount(convertedId).getVolume(getTimeSlice(time)) == null){
			set(this.alighting, stopFacilityId, time, 0);
		}
		if(this.boarding.getCount(convertedId).getVolume(getTimeSlice(time)) == null){
			set(this.boarding, stopFacilityId, time, 0);
		}
		increase(this.capacity, stopFacilityId, time, vehCapacity);
		increase(this.totalPax, stopFacilityId, time, nrSeatsInUse);
		Integer slice = getTimeSlice(time);
		set(this.occupancy, stopFacilityId, time, this.totalPax.getCount(convertedId).getVolume(slice).getValue() /
				this.capacity.getCount(convertedId).getVolume(slice).getValue());
	}
	
	private void increase(Counts counts, Id<TransitStopFacility> stopFacilityId, Double time, double increaseBy){
		Count count = counts.getCount(Id.create(stopFacilityId, Link.class));
		Integer slice = getTimeSlice(time);
		Volume v;
		if(count.getVolumes().containsKey(slice)){
			v = count.getVolume(slice);
		}else{
			v = count.createVolume(slice, 0);
		}
		v.setValue(v.getValue() + increaseBy);
	}
	
	private void set(Counts counts, Id<TransitStopFacility> stopFacilityId, Double time, double value){
		Count count =  counts.getCount(Id.create(stopFacilityId, Link.class));
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
		int slice =  (int) (time / this.interval) + 1;
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
