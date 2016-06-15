/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.usecases.kraptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitRouteStopImpl;
import org.matsim.pt.transitSchedule.TransitStopFacilityImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author nagel
 *
 */
public class KRaptor {
	private final static int lastRound = 10 ;
	
	List<DecoratedTransitStop> markedStops = new ArrayList<>() ;
	Map<TransitRoute,DecoratedTransitStop> routeMap = new HashMap<>() ;
	
	void run() {
		DecoratedTransitStop initialStop = null ;
		initialStop.currentlyBestTime = 0. ;
		initialStop.bestTime[0] = 0. ;
		markedStops.add( initialStop ) ; 
		
		for ( int round = 0 ; round <= lastRound ; round++ ) {
			doRound( round ) ;
		}
	}
	
	void doRound( int round ) {
		routeMap.clear() ;
		
		accumulateRoutes() ;
		
		traverseRoutes() ;
		
		lookAtFootpaths() ;
		
	}
	
	private void lookAtFootpaths() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	void accumulateRoutes() {
		for ( DecoratedTransitStop stop : markedStops ) {
			for ( TransitRoute route : stop.getOutgoingRoutes() ) {
				DecoratedTransitStop prevHopOnStation = routeMap.get( route ) ;
				if ( prevHopOnStation==null || stop.compareTo(prevHopOnStation) < 0 ) {
					routeMap.put( route, stop ) ;
				}
			}
		}
		markedStops.clear(); 
	}
	
	void traverseRoutes() {
		for ( Entry<TransitRoute, DecoratedTransitStop> entry : routeMap.entrySet() ) {
			TransitRoute route = entry.getKey() ;
			DecoratedTransitStop hopOnStop = entry.getValue() ;
			
			double currentTime = hopOnStop.currentlyBestTime ;
			
			Departure theDeparture ;
			for ( Departure departure : route.getDepartures().values() ) {
				if ( departure.getDepartureTime() + hopOnStop.getDepartureOffset() >= currentTime ) {
					// we can catch this departure
					theDeparture = departure ;
					break ;
				}
			}
			
//			boolean active = false ;
//			for ( DecoratedTransitStop stop : route.getStops() ) {
//				if ( stop.equals( hopOnStop ) ) {
//					active = true ;
//				}
//				if ( active ) {
//					if ( stop.
//				}
//			}
			
			
		}
	}

	private static class DecoratedTransitStop extends TransitRouteStopImpl implements Comparable<DecoratedTransitStop> {

		protected DecoratedTransitStop(TransitStopFacility stop, double arrivalDelay, double departureDelay) {
			super(stop, arrivalDelay, departureDelay);
		}

		List<TransitRoute> outgoingRoutes = new ArrayList<>() ;
		double currentlyBestTime ;
		double[] bestTime = new double[lastRound+1] ;

		List<TransitRoute> getOutgoingRoutes() {
			return outgoingRoutes ;
		}

		@Override
		public int compareTo(DecoratedTransitStop o) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}
		
	}
}
