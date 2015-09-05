/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateCircularScenarioWithCompleteSocialNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts.scenariohandling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.contrib.socnetsim.utils.CollectionUtils;
import playground.thibautd.utils.DesiresConverter;

/**
 * @author thibautd
 */
public class GenerateCircularScenarioWithCompleteSocialNetwork {
	public static void main(final String[] args) throws IOException {
		final ArgParser argParser = new ArgParser();
		argParser.setDefaultValue( "--radius" , "10000" );
		argParser.setDefaultValue( "--nlinks" , "100" );
		argParser.setDefaultValue( "--nagentsperlink" , "1" );
		argParser.setDefaultValue( "--nseparationlinks" , "1" );
		argParser.setDefaultValue( "--degreesmallworld" , "20" );

		final Args parsed = argParser.parseArgs( args );
		final int nLinks = Integer.parseInt( parsed.getValue( "--nlinks" ) );
		final int nAgentsPerLink = Integer.parseInt( parsed.getValue( "--nagentsperlink" ) );
		final int nSeparationLinks = Integer.parseInt( parsed.getValue( "--nseparationlinks" ) );
		final int smallWorldDegree = Integer.parseInt( parsed.getValue( "--degreesmallworld" ) );
		final double radius = Integer.parseInt( parsed.getValue( "--radius" ) );

		final String outputDirectory = parsed.getNonSwitchedArgs()[ 0 ];
		createDirectory( outputDirectory );

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		final Network net = scenario.getNetwork();
		final ActivityFacilities facilities = scenario.getActivityFacilities();
		final Population population = scenario.getPopulation();

		final double angleStep = 360d / nLinks;
		Node firstNode = null;
		Node lastNode = null;
		for ( int i = 0; i < nLinks; i++ ) {
			final double angle = i * angleStep;
			final Node newNode = net.getFactory().createNode(
					Id.create( "node-"+angle , Node.class),
					new CoordImpl(
						radius * Math.cos( Math.PI * angle / 180 ),
						radius * Math.sin( Math.PI * angle / 180 ) ) );
			net.addNode( newNode );
			if ( firstNode == null ) firstNode = newNode;

			if ( lastNode != null ) {
				final Link l = createAndAddLink( net , lastNode , newNode );
				createAndAddLink( net , newNode , lastNode );

				if ( i % nSeparationLinks == 0 ) {
					createAndAddFacility( facilities , l );
				}
			}

			lastNode = newNode;
		}

		/* connect two ends */ {
			final Link l = createAndAddLink( net , firstNode , lastNode );
			createAndAddLink( net , lastNode , firstNode );

			if ( nLinks % nSeparationLinks == 0 ) {
				createAndAddFacility( facilities , l );
			}
		}

		final Random random = new Random( 4567 );
		final Desires desires = new Desires( null );
		desires.putActivityDuration( "home" , 14 * 3600 );
		desires.putActivityDuration( "leisure" , 10 * 3600 );
		for ( ActivityFacility facility : facilities.getFacilities().values() ) {
			for ( int i = 0; i < nAgentsPerLink; i++ ) {
				final Person person = population.getFactory().createPerson( Id.create( facility.getId()+"-"+i , Person.class) );
				population.addPerson( person );
				final Plan plan = population.getFactory().createPlan();
				plan.setPerson( person );
				person.addPlan( plan );

				plan.addActivity(
						createActivity(
							population.getFactory(),
							"home",
							facility,
							8 * 3600d ) );
				plan.addLeg( population.getFactory().createLeg( TransportMode.car ) );
				plan.addActivity(
						createActivity(
							population.getFactory(),
							"leisure",
							CollectionUtils.getRandomElement(
								random,
								facilities.getFacilities() ).getValue(),
							18 * 3600d ) );
				plan.addLeg( population.getFactory().createLeg( TransportMode.car ) );
				plan.addActivity(
						createActivity(
							population.getFactory(),
							"home",
							facility,
							Time.UNDEFINED_TIME ) );

				population.getPersonAttributes().putAttribute(
						""+person.getId(),
						"desires",
						desires );
			}
		}


		new SocialNetworkWriter( createCompleteSocialNetwork( population ) ).write( outputDirectory+"/complete_social_network.xml.gz" );
		new SocialNetworkWriter( createSmallWorldSocialNetwork( population , smallWorldDegree ) ).write( outputDirectory+"/smallworld_social_network.xml.gz" );

		new NetworkWriter( net ).write( outputDirectory+"/network.xml.gz" );
		new PopulationWriter( population , net ).write( outputDirectory+"/plans.xml.gz" );
		new FacilitiesWriter( facilities ).write( outputDirectory+"/facilities.xml.gz" );

		// now this stupid f2l file
		final BufferedWriter f2lw = IOUtils.getBufferedWriter( outputDirectory+"/f2l.f2l" );
		f2lw.write("fid\tlid");
		for ( ActivityFacility f : facilities.getFacilities().values() ) {
			f2lw.newLine();
			f2lw.write( f.getId()+"\t"+f.getLinkId() );
		}
		f2lw.close();

		final ObjectAttributesXmlWriter attWriter =
			new ObjectAttributesXmlWriter( population.getPersonAttributes() );
		attWriter.putAttributeConverter(
				Desires.class,
				new DesiresConverter() );
		attWriter.writeFile( outputDirectory+"/person_attributes.xml.gz" );
	}

	private static SocialNetwork createSmallWorldSocialNetwork(
			final Population population,
			final int degree) {
		final Person[] persons =
			population.getPersons().values().toArray(
					new Person[ population.getPersons().size() ] );

		final SocialNetwork net = new SocialNetworkImpl( true );

		for ( Person p : persons ) net.addEgo( p.getId() );

		for ( int i=0; i < persons.length; i++ ) {
			for ( int j=i+1; j < i + ( degree / 2) ; j++ ) {
				net.addBidirectionalTie(
						persons[ i ].getId(),
						persons[ j % persons.length ].getId() );
			}
		}

		// TODO: rewire

		return net;
	}

	private static SocialNetwork createCompleteSocialNetwork(
			final Population population) {
		final Person[] persons =
			population.getPersons().values().toArray(
					new Person[ population.getPersons().size() ] );

		final SocialNetwork net = new SocialNetworkImpl( true );

		for ( Person p : persons ) net.addEgo( p.getId() );

		for ( int i=0; i < persons.length; i++ ) {
			for ( int j=i+1; j < persons.length; j++ ) {
				net.addBidirectionalTie(
						persons[ i ].getId(),
						persons[ j ].getId() );
			}
		}

		return net;
	}

	private static void createDirectory(final String outputDirectory) {
		final File file = new File( outputDirectory );
		if ( file.exists() ) {
			if ( !file.isDirectory() ) {
				throw new RuntimeException( outputDirectory+" exists and is a file" );
			}
			if ( file.listFiles().length > 0 ) {
				throw new RuntimeException( outputDirectory+" exists and is not empty" );
			}
		}
		else {
			file.mkdirs();
		}
	}

	private static Activity createActivity(
			final PopulationFactory factory,
			final String type,
			final ActivityFacility facility,
			final double time) {
		final Activity activity =
			factory.createActivityFromLinkId(
					type,
					facility.getLinkId() );
		((ActivityImpl) activity).setCoord( facility.getCoord() );
		((ActivityImpl) activity).setFacilityId( facility.getId() );

		activity.setEndTime( time );

		return activity;
	}

	private static void createAndAddFacility(
			final ActivityFacilities facilities,
			final Link l) {
		final ActivityFacility facility =
			facilities.getFactory().createActivityFacility(
					Id.create(l.getId().toString(), ActivityFacility.class),
					l.getCoord() );
		((ActivityFacilityImpl) facility).setLinkId( l.getId() );
		facilities.addActivityFacility( facility );

		final ActivityOption home = facilities.getFactory().createActivityOption( "home" );
		facility.addActivityOption( home );

		final ActivityOption leisure = facilities.getFactory().createActivityOption( "leisure" );
		facility.addActivityOption( leisure );
	}

	private static Link createAndAddLink( 
			final Network net,
			final Node from,
			final Node to) {
		final Link l =
			net.getFactory().createLink(
				Id.create( from.getId() +"^"+ to.getId() , Link.class),
				from,
				to );
		l.setLength(
				CoordUtils.calcDistance(
					from.getCoord(),
					to.getCoord() ) );
		l.setFreespeed( 100000 / 3600d ); // 100 km/h
		net.addLink( l );
		return l;
	}
}

