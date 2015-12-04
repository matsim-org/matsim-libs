/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePseudoTransitNetwork.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;

/**
 * @author thibautd
 */
public class CreatePseudoTransitNetwork {
	public static void main( String[] args ) {
		final String inputSchedule = args[ 0 ];
		final String outputSchedule = args[ 1 ];
		final String outputNetwork = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new TransitScheduleReader( sc ).readFile( inputSchedule );
		new CreatePseudoNetwork(sc.getTransitSchedule(), sc.getNetwork(), "tr_").createNetwork();
		new NetworkWriter( sc.getNetwork() ).write( outputNetwork );
		new TransitScheduleWriter( sc.getTransitSchedule() ).writeFile( outputSchedule );
	}
}

