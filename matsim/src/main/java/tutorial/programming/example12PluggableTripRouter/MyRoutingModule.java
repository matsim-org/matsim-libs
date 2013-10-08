/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package tutorial.programming.example12PluggableTripRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;

/**
 * @author nagel
 *
 */
public class MyRoutingModule implements RoutingModule, BasicEventHandler {

	private Map<Id, Double> enterEvents = new ConcurrentHashMap<Id,Double>() ;

	private List<Double> sum = new ArrayList<Double>() ;
	private List<Double> cnt = new ArrayList<Double>() ;

	private int lastBin;
	
	MyRoutingModule() {
		lastBin = 36*4 ;
		for ( int ii=0 ; ii<=lastBin; ii++ ) {
			sum.add(0.) ;
			cnt.add(0.) ;
		}
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(Event event) {
		if ( event instanceof LinkEnterEvent ) {
			LinkEnterEvent ev = (LinkEnterEvent) event ;
			enterEvents.put( ev.getVehicleId() , ev.getTime() ) ;
		} else if (event instanceof LinkLeaveEvent ) {
			LinkLeaveEvent ev = (LinkLeaveEvent) event ;
			Double linkEnterTime = enterEvents.get( ev.getVehicleId() ) ; 
			if ( linkEnterTime != null ) {

				double ttime = ev.getTime() - linkEnterTime ;

				int bin = time2bin( linkEnterTime ) ;
				
				if ( bin > lastBin ) {
					for (int ii = lastBin+1 ; ii<=bin; ii++ ) {
						sum.add(0. ) ;
						cnt.add(0. ) ;
					}
					lastBin = bin ;
				}
				
				double oldTtime = sum.get( bin ) ;
				sum.set(bin, oldTtime+ttime ) ;

				double oldCnt = cnt.get( bin ) ;
				cnt.set( bin, oldCnt+1 ) ;

				enterEvents.remove( ev.getVehicleId() ) ;
			}
		} else if ( event instanceof VehicleArrivesAtFacilityEvent ) { // is this also thrown when entering parking???  kai, may'13
			VehicleArrivesAtFacilityEvent ev = (VehicleArrivesAtFacilityEvent) event ;
			Double linkEnterTime = enterEvents.get( ev.getVehicleId() ) ; 
			if ( linkEnterTime != null ) {
				enterEvents.remove( ev.getVehicleId() ) ; // i.e. do not use
			}
		} else if ( event instanceof VehicleDepartsAtFacilityEvent ) {
			// ...
		}
		
	}

	private int time2bin(Double linkEnterTime) {
		return (int) (linkEnterTime/900.) ;
	}

}
