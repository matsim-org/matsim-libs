/* *********************************************************************** *
 * project: org.matsim.*
 * ComparePerformanceMultimodalRoutingWithAndWithoutSubnetworks.java
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
package playground.thibautd.scripts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author thibautd
 */
public class ComparePerformanceMultimodalRoutingWithAndWithoutSubnetworks {
	public static void main(final String[] args) {
		final String netFile = args[ 0 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).readFile( netFile );

		final List<Tuple<Node,Node>> ods = sampleOds( sc.getNetwork() );

        final FreespeedTravelTimeAndDisutility ptTimeCostCalc =
                new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
        final IntermodalLeastCostPathCalculator routingAlgo = (IntermodalLeastCostPathCalculator)
                new DijkstraFactory().createPathCalculator(
						sc.getNetwork(),
                        ptTimeCostCalc,
                        ptTimeCostCalc);

		final Person person = PersonImpl.createPerson(null);

		final Timer timer = new Timer();
		timer.startTimer();
		for ( final Tuple<Node, Node> od : ods ) {
			final Node o = od.getFirst();
			final Node d = od.getSecond();

			routingAlgo.calcLeastCostPath( o , d , 12 , person , null );
		}
		timer.endTimer();
		timer.printMeasuredTime( "normal " );

		routingAlgo.setModeRestriction( Collections.singleton( "car" ) );
		timer.resetTimer();
		timer.startTimer();
		for ( final Tuple<Node, Node> od : ods ) {
			final Node o = od.getFirst();
			final Node d = od.getSecond();

			routingAlgo.calcLeastCostPath( o , d , 12 , person , null );
		}
		timer.endTimer();
		timer.printMeasuredTime( "restricted " );
	}

	private static List<Tuple<Node, Node>> sampleOds(final Network network) {
		final List<Tuple<Node, Node>> list = new ArrayList<Tuple<Node, Node>>();

		final List<Node> nodes = new ArrayList<Node>( network.getNodes().values() );
		final Random r = new Random( 1234 );
		for ( int i=0; i < 500; i++ ) {
			list.add( new Tuple<Node, Node>(
						nodes.get( r.nextInt( nodes.size() ) ),
						nodes.get( r.nextInt( nodes.size() ) ) ) );
		}

		return list;
	}
}

