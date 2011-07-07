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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

class EventsAnalyser {
	
	class MyEventHandler1 implements AgentDepartureEventHandler, AgentArrivalEventHandler {
		
		MyEventHandler1() {
		}
		
		private Map<Id,AgentDepartureEvent> departureMap = new HashMap<Id,AgentDepartureEvent>() ;

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			departureMap.put( event.getPersonId(), event ) ;
		}

		private double timeSumCar = 0. ;
		private double cntCar = 0. ;
		
		private double timeSumPt = 0. ;
		private double cntPt = 0. ;
		
		@Override
		public void handleEvent(AgentArrivalEvent arEvent) {
			if ( !departureMap.containsKey( arEvent.getPersonId() )) {
				Logger.getLogger(this.getClass()).warn("no correspnding departure event found; dropping arrival event") ;
			} else {
				AgentDepartureEvent dpEvent = departureMap.get( arEvent.getPersonId() ) ;
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
