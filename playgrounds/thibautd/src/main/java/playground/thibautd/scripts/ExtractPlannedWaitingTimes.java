/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractPlannedWaitingTimes.java
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
package playground.thibautd.scripts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author thibautd
 */
public class ExtractPlannedWaitingTimes {
	private static final Logger log =
		Logger.getLogger(ExtractPlannedWaitingTimes.class);

	private static final String PLANS = "plans";
	private static final String PERSON = "person";
	private static final String ID = "id";
	private static final String PLAN = "plan";
	private static final String SELECTED = "selected";
	private static final String SELECTED_IS_TRUE = "yes";
	private static final String ACTIVITY = "act";
	private static final String LEG = "leg";
	private static final String ROUTE = "route";
	private static final String DUR = "dur";
	private static final String END = "end_time";
	private static final String TRAV_TIME = "trav_time";
	private static final String MODE = "mode";

	public static void main(final String[] args) {
		String plansFile = args[ 0 ];
		String outFile = args[ 1 ];

		PlansFileParser parser = new PlansFileParser();
		parser.parse( plansFile );
		try {
			parser.getTimes().writeWaitingTimesToFile( outFile );
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private static final class PlansFileParser extends MatsimXmlParser {
		private JointTimes jointTimes = null;
		private double now = Double.NaN;
		private boolean thisPlanIsToAnalyse = false;
		private Id currentId = null;
		private double currentTravelTime = Double.NaN;
		private String currentMode = null;
		private Counter counter;

		@Override
		public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
			if (PLANS.equals( name )) {
				jointTimes = new JointTimes();
				counter = new Counter( "parsing person # " );
			}
			else if ( PERSON.equals( name ) ) {
				counter.incCounter();
				currentId = Id.create( atts.getValue( ID ).trim() , Person.class );
			}
			else if ( PLAN.equals( name ) ) {
				if ( SELECTED_IS_TRUE.equals( atts.getValue( SELECTED ).trim() ) ) {
					thisPlanIsToAnalyse = true;
					now = 0;
				}
				else {
					thisPlanIsToAnalyse = false;
					now = Double.NaN;
				}
			}
			else if ( thisPlanIsToAnalyse ) {
				if (ACTIVITY.equals( name )) {
					handleActivity( atts );
				}
				else if (LEG.equals( name )) {
					handleLeg( atts );
				}
			}
		}

		private void handleLeg(final Attributes atts) {
			// only update "now" at the end, so that we have the departure time
			// when handling the route
			currentTravelTime = Time.parseTime( atts.getValue( TRAV_TIME ) );
			currentMode = atts.getValue( MODE );
		}

		private void handleActivity(final Attributes atts) {
			String dur = atts.getValue( DUR );
			String end = atts.getValue( END );

			if ( end != null) {
				now = Time.parseTime( end );
			}
			else {
				now += Time.parseTime( dur );
			}
		}

		@Override
		public void endTag(
				final String name,
				final String content,
				final Stack<String> context) {
			if (LEG.equals( name )) {
				now += currentTravelTime;
				currentTravelTime = Double.NaN;
				currentMode = null;
			}
			else if (thisPlanIsToAnalyse && ROUTE.equals( name )) {
				handleRoute( content );
			}
			else if ( PLANS.equals( name ) ) {
				counter.printCounter();
			}
		}

		private void handleRoute(final String content) {
			if (currentMode.equals( JointActingTypes.DRIVER )) {
				// we do not care of ODs
				DriverRoute route = new DriverRoute( (Id) null , null );
				route.setRouteDescription(content);

				jointTimes.notifyDriverStartTime( currentId , route.getPassengersIds() , now );
			}
			else if (currentMode.equals( JointActingTypes.PASSENGER )) {
				PassengerRoute route = new PassengerRoute( null , null );
				route.setRouteDescription(content);
				jointTimes.notifyPassengerStartTime( currentId , route.getDriverId() , now );
			}
		}

		public JointTimes getTimes() {
			return jointTimes;
		}
	}

	private static class JointTimes {
		private final Map< Tuple<Id, Id> , List<Double> > driverDepartures = new HashMap< Tuple<Id,Id>, List<Double> >();
		private final Map< Tuple<Id, Id> , List<Double> > passengerDepartures = new HashMap< Tuple<Id,Id>, List<Double> >();
		
		public void notifyDriverStartTime(
				final Id<Person> driverId,
				final Collection<Id<Person>> passengersIds,
				final double time) {
			for (Id passenger : passengersIds) {
				Tuple<Id, Id> tuple = new Tuple<Id, Id>( driverId , passenger );
				List<Double> times = driverDepartures.get( tuple );
				if (times == null) {
					times = new ArrayList<Double>();
					driverDepartures.put( tuple , times );
				}
				times.add( time );
			}
		}

		public void notifyPassengerStartTime(
				final Id passengerId,
				final Id driverId,
				final double time) {
			Tuple<Id, Id> tuple = new Tuple<Id, Id>( driverId , passengerId );
			List<Double> times = passengerDepartures.get( tuple );
			if (times == null) {
				times = new ArrayList<Double>();
				passengerDepartures.put( tuple , times );
			}
			times.add( time );
		}

		public void writeWaitingTimesToFile(final String file) throws IOException {
			BufferedWriter writer = IOUtils.getBufferedWriter( file );

			log.warn( "the produced results will be correct only if one unique passenger per driver trip!" );
			writer.write( "id\tstatus\twaiting_s" );
			Counter counter = new Counter( "writing departure # " );
			for (Map.Entry< Tuple<Id, Id> , List<Double> > entry : driverDepartures.entrySet()) {
				Id driverId = entry.getKey().getFirst();
				Id passengerId = entry.getKey().getSecond();
				Iterator<Double> passengerTimes = passengerDepartures.get( entry.getKey() ).iterator();
				Iterator<Double> driverTimes = entry.getValue().iterator();

				while (driverTimes.hasNext()) {
					double d = driverTimes.next();
					double p = passengerTimes.next();

					counter.incCounter();
					writer.newLine();
					writer.write( driverId + "\tdriver\t" + ( d < p ? p-d : 0 ) );
					writer.newLine();
					writer.write( passengerId + "\tpassenger\t" + ( p < d ? d-p : 0 ) );
				}
			}

			counter.printCounter();
			writer.close();
		}
	}
}

