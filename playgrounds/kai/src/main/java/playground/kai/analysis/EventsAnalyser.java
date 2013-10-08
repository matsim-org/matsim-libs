/* *********************************************************************** *
 * project: kai
 * EventsReduce.java
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

package playground.kai.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

class EventsAnalyser {
	
	class MyEventHandler1 implements PersonDepartureEventHandler, PersonArrivalEventHandler {
		
		MyEventHandler1() {
		}
		
		private Map<Id,PersonDepartureEvent> departureMap = new HashMap<Id,PersonDepartureEvent>() ;

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			departureMap.put( event.getPersonId(), event ) ;
		}

		private double timeSumCar = 0. ;
		private double cntCar = 0. ;
		
		private double timeSumPt = 0. ;
		private double cntPt = 0. ;
		
		@Override
		public void handleEvent(PersonArrivalEvent arEvent) {
			if ( !departureMap.containsKey( arEvent.getPersonId() )) {
				Logger.getLogger(this.getClass()).warn("no correspnding departure event found; dropping arrival event") ;
			} else {
				PersonDepartureEvent dpEvent = departureMap.get( arEvent.getPersonId() ) ;
				double ttime = arEvent.getTime() - dpEvent.getTime();
				String mode = arEvent.getLegMode() ;
				if ( mode.equals("car") ) {
					cntCar ++ ;
					timeSumCar += ttime ;
				} else if ( mode.equals("pt") ) {
					cntPt ++ ;
					timeSumPt += ttime ;
				}
			}
		}
		
		void finish() {
			double cnt = 0. ;
			double sumTtimes = 0. ;
			
			double avCarTtime = timeSumCar/cntCar ;
			System.out.println( "number of car legs: " + cntCar + "; av ttime: " + avCarTtime ) ;
			cnt += cntCar ;
			sumTtimes += timeSumCar ;

			double avPtTtime = timeSumPt/cntPt ;
			System.out.println( "number of pt  legs: " + cntPt  + "; av ttime: " + avPtTtime ) ;
			cnt += cntPt ;
			sumTtimes += timeSumPt ;

			double avAllTtime = sumTtimes/cnt ;
			System.out.println( "number of above legs: " + cnt  + "; av ttime: " + avAllTtime ) ;
		}

		@Override
		public void reset(int iteration) {
			throw new UnsupportedOperationException();
		}

	}
	private void run() {
		String inputFile = "/Users/nagel/kairuns/16ba-ext-30jun/ITERS/it.100/reduced.events.xml.gz";

		EventsManager eventsIn = EventsUtils.createEventsManager();

		MyEventHandler1 handler = new MyEventHandler1();
		eventsIn.addHandler(handler);

		MatsimEventsReader reader = new MatsimEventsReader(eventsIn);
		reader.readFile(inputFile);
		
		handler.finish() ;
		
		System.out.println("done");
	}

	public static void main(String[] args) {
		new EventsAnalyser().run() ;
	}

}
