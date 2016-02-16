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
package playground.ivt.maxess.nestedlogitaccessibility.scripts;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.ivt.utils.MoreIOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class GenerateSimpleDummyExample {
	private static final Coord CENTER = new Coord( 0 , 0 );
	private static final double RADIUS = 50 * 1000;
	private static final double STEP = 1000;
	private static final int N_RADII = 60;

	private static final int SIZE_SUBPOP = 500;
	private static final double CENTER_POP = 40 * 1000;
	private static final double RADIUS_POP = 10 * 1000;

	private static final double RADIUS_CBD = 20 * 1000;
	private static final int N_FACILITIES = 300;

	private static final double FREESPEED = 70 * 1000 / 3600;
	private static final double SPEED_PT = 150 * 1000 / 3600;
	private static final double PT_STEP = 1000;

	public static void main( final String... args ) {
		final String outputDirectory = args[ 0 ];

		MoreIOUtils.initOut( outputDirectory );

		try {
			final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			generateNetwork( scenario );
			generatePopulation( scenario );
			generateFacilities( scenario );
			generatePT( scenario );

			final Config config = ConfigUtils.createConfig();
			new NetworkWriter( scenario.getNetwork() ).write( outputDirectory + "/network.xml.gz" );
			config.network().setInputFile( outputDirectory + "/network.xml.gz" );

			new PopulationWriter( scenario.getPopulation() , scenario.getNetwork() ).write( outputDirectory + "/population.xml.gz" );
			config.plans().setInputFile( outputDirectory + "/population.xml.gz" );
			new ObjectAttributesXmlWriter( scenario.getPopulation().getPersonAttributes() ).writeFile( outputDirectory + "/personAttributes.xml.gz" );
			config.plans().setInputPersonAttributeFile( outputDirectory + "/personAttributes.xml.gz" );

			new FacilitiesWriter( scenario.getActivityFacilities() ).write( outputDirectory + "/facilities.xml.gz" );
			config.facilities().setInputFile( outputDirectory + "/facilities.xml.gz" );

			new TransitScheduleWriter( scenario.getTransitSchedule() ).writeFile( outputDirectory + "/pt_schedule.xml.gz" );
			config.transit().setTransitScheduleFile( outputDirectory + "/pt_schedule.xml.gz" );
			config.transit().setUseTransit( true );
			// set search radius to 1m: will only look at closest station (even if further away than 1m),
			// making computation faster (city center is dense, but it does not make sense to look at other
			// stops than the closest given the topology. Default of 1km results in lots of options)
			config.transitRouter().setSearchRadius( 10 );

			new ConfigWriter( config ).write( outputDirectory+"/config.xml.gz" );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	private static void generateFacilities( Scenario scenario ) {
		final ActivityFacilities facilities = scenario.getActivityFacilities();
		final ActivityFacilitiesFactory factory = facilities.getFactory();

		final Random random = new Random( 1234 );
		for ( int i=0; i < N_FACILITIES; i++ ) {
			final ActivityFacility f =
					factory.createActivityFacility(
						Id.create( i , ActivityFacility.class ),
							randomCoord(
									random,
									CENTER,
									RADIUS_CBD ));
			facilities.addActivityFacility( f );
			f.addActivityOption( factory.createActivityOption( "leisure" ) );
		}
	}

	private static void generatePT( Scenario scenario ) {
		final double angleBigLine = 2 * Math.PI / 3;
		generateLine(
				scenario,
				angleBigLine,
				RADIUS );

		for ( int i=1; i < N_RADII; i++ ) {
			generateLine(
					scenario,
					angleBigLine + i * (2 * Math.PI / N_RADII),
					RADIUS_CBD );
		}

		new CreatePseudoNetwork(
				scenario.getTransitSchedule(),
				scenario.getNetwork(),
				"pt-" ).createNetwork();
	}


	private static void generateLine(
			final Scenario scenario,
			final double angle,
			final double radius ) {
		final TransitSchedule schedule = scenario.getTransitSchedule();
		final TransitScheduleFactory factory = schedule.getFactory();

		final double xStep = Math.cos( angle ) * PT_STEP;
		final double yStep = Math.sin( angle ) * PT_STEP;

		final TransitLine line =
				factory.createTransitLine(
						Id.create(
								"line "+angle,
								TransitLine.class ) );

		final List<TransitRouteStop> outboundStops = new ArrayList<>();
		final List<TransitRouteStop> inboundStops = new ArrayList<>();

		final double arrivalDelay = PT_STEP / SPEED_PT;

		Coord coord = CENTER;
		double time = 0;
		for ( double d = 0; d < radius; d += PT_STEP ) {
			final TransitStopFacility stop =
					factory.createTransitStopFacility(
						Id.create( angle +"-" + d, TransitStopFacility.class ),
						coord,
						false );
			schedule.addStopFacility( stop );

			outboundStops.add( factory.createTransitRouteStop( stop, time, time ) );
			inboundStops.add( 0, factory.createTransitRouteStop( stop, time, time ) );
			time += arrivalDelay;

			coord =	new Coord(
						coord.getX() + xStep,
						coord.getY() + yStep );
		}

		final TransitRoute inboundRoute = factory.createTransitRoute(
				Id.create(
						"inbound",
						TransitRoute.class ),
				null,
				inboundStops,
				"pt" );
		final TransitRoute outboundRoute = factory.createTransitRoute(
				Id.create(
						"outbound",
						TransitRoute.class ),
				null,
				outboundStops,
				"pt" );

		for ( double t = 0; t <= 24 * 3600; t += 600) {
			inboundRoute.addDeparture(
					factory.createDeparture(
							Id.create(
									"inbound-"+Time.writeTime( t ),
									Departure.class ),
							t ));
			outboundRoute.addDeparture(
					factory.createDeparture(
							Id.create(
									"outbound-"+Time.writeTime( t ),
									Departure.class ),
							t ));
		}

		line.addRoute( inboundRoute );
		line.addRoute( outboundRoute );
		schedule.addTransitLine( line );
	}

	private static void generatePopulation( Scenario scenario ) {
		final Population population = scenario.getPopulation();
		final PopulationFactory f = population.getFactory();

		final Random random = new Random( 123 );
		Coord center = new Coord( CENTER_POP, 0 );
		for ( int i=0; i < SIZE_SUBPOP; i++ ) {
			final Coord coord =
					randomCoord(
							random,
							center,
							RADIUS_POP );
			final Person p = f.createPerson( Id.createPersonId( "car-"+i ) );
			population.addPerson( p );
			localizePerson( f, p, coord );
			new AttributesAdder()
					.withCarAvailable( true )
					.fill( p, population.getPersonAttributes() );
		}

		center = new Coord(
				Math.cos( 2 * Math.PI / 3 ) * CENTER_POP,
				Math.sin( 2 * Math.PI / 3 ) * CENTER_POP );
		for ( int i=0; i < SIZE_SUBPOP; i++ ) {
			final Coord coord =
					randomCoord(
							random,
							center,
							RADIUS_POP );
			final Person p = f.createPerson( Id.createPersonId( "nocar-pt-"+i ) );
			population.addPerson( p );
			localizePerson( f, p, coord );
			new AttributesAdder()
					.withCarAvailable( false )
					.fill( p, population.getPersonAttributes() );
		}

		center = new Coord(
				Math.cos( 4 * Math.PI / 3 ) * CENTER_POP,
				Math.sin( 4 * Math.PI / 3 ) * CENTER_POP );
		for ( int i=0; i < SIZE_SUBPOP; i++ ) {
			final Coord coord =
					randomCoord(
							random,
							center,
							RADIUS_POP );
			final Person p = f.createPerson( Id.createPersonId( "nocar-"+i ) );
			population.addPerson( p );
			localizePerson( f, p, coord );
			new AttributesAdder()
					.withCarAvailable( false )
					.fill( p, population.getPersonAttributes() );
		}
	}

	private static void localizePerson( PopulationFactory f, Person p, Coord coord ) {
		final Activity home = f.createActivityFromCoord( "home" , coord );
		p.addPlan( f.createPlan() );
		p.getSelectedPlan().addActivity( home );
	}

	private static Coord randomCoord( Random random, Coord center, double radiusPop ) {
		final double randomAngle = random.nextDouble() * 2 * Math.PI;
		final double randomDistance = Math.sqrt( random.nextDouble() ) * radiusPop;

		// TODO: make density constant? Here, decreases with distance to center.
		return new Coord(
				center.getX() + randomDistance * Math.cos( randomAngle ),
				center.getY() + randomDistance * Math.sin( randomAngle ) );
	}

	private static void generateNetwork( Scenario scenario ) {
		final Network net = scenario.getNetwork();
		final NetworkFactory f = net.getFactory();

		int nodeId = 0;
		final Node centerNode = f.createNode( Id.createNodeId( nodeId++ ) , CENTER );
		net.addNode( centerNode );

		int linkId = 0;
		for ( int i=0; i < N_RADII; i++ ) {
			Node lastNode = centerNode;

			final double xStep = STEP * Math.cos( i * (2 * Math.PI / N_RADII) );
			final double yStep = STEP * Math.sin( i * (2 * Math.PI / N_RADII) );

			for ( double dist = 0; dist <= RADIUS; dist += STEP ) {
				final Node newNode = f.createNode(
						Id.createNodeId( nodeId++ ),
						new Coord(
								lastNode.getCoord().getX() + xStep,
								lastNode.getCoord().getY() + yStep ));
				net.addNode( newNode );
				net.addLink(
						f.createLink(
								Id.createLinkId( linkId++ ),
								lastNode,
								newNode ) );
				net.addLink(
						f.createLink(
								Id.createLinkId( linkId++ ),
								newNode,
								lastNode ) );
				lastNode = newNode;
			}

			for ( Link l : net.getLinks().values() ) {
				l.setFreespeed( FREESPEED );
				l.setLength(
						CoordUtils.calcEuclideanDistance(
								l.getFromNode().getCoord(),
								l.getToNode().getCoord() ) );
			}
		}
	}

	private static class AttributesAdder {
		private boolean carAvailable = true;

		public AttributesAdder withCarAvailable( final boolean carAvailable ) {
			this.carAvailable = carAvailable;
			return this;
		}

		public void fill( Person p , ObjectAttributes att ) {
			att.putAttribute( p.getId().toString(),
					"availability: bicycle",
					"always" );

			att.putAttribute( p.getId().toString(),
					"availability: car",
					carAvailable ? "always" : "never" );

			att.putAttribute( p.getId().toString(),
					"driving licence",
					"yes" );

			att.putAttribute( p.getId().toString(),
					"abonnement: Verbund",
					"no" );

			att.putAttribute( p.getId().toString(),
					"abonnement: Halbtax",
					"yes" );

			att.putAttribute( p.getId().toString(),
					"abonnement: GA first class",
					"no" );

			att.putAttribute( p.getId().toString(),
					"abonnement: GA second class",
					"no" );
		}
	}
}
