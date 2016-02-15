/* *********************************************************************** *
 * project: org.matsim.*
 * SampleHouseholds.java
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
package playground.thibautd.scripts.scenariohandling;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;

import java.util.Random;

/**
 * @author thibautd
 */
public class SampleHouseholds {
	private static final Logger log =
		Logger.getLogger(SampleHouseholds.class);

	public static void main(final String[] args) {
		final double pAccept = Double.parseDouble( args[ 0 ] );
		if ( pAccept < 0 || pAccept > 1 ) throw new IllegalArgumentException( "wrong rate "+pAccept );
		final String inHouseholdFile = args[ 1 ];
		final String inPopFile = args[ 2 ];
		final String outHouseholdFile = args[ 3 ];
		final String outPopFile = args[ 4 ];

		final Config config = ConfigUtils.createConfig();
		config.scenario().setUseHouseholds( true );
		final Scenario sc = ScenarioUtils.createScenario( config );
		new HouseholdsReaderV10( ((MutableScenario) sc).getHouseholds() ).parse( inHouseholdFile );
		new MatsimPopulationReader( sc ).parse( inPopFile );

		final HouseholdsImpl newHouseholds = new HouseholdsImpl();
        MutableScenario sc1 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        final Population newPopulation = PopulationUtils.createPopulation(sc1.getConfig(), sc1.getNetwork());

		final Random random = new Random( 8904567 );
		int countAcceptedHouseholds = 0;
		int countAcceptedPersons = 0;
		for ( Household hh : ((MutableScenario) sc).getHouseholds().getHouseholds().values() ) {
			if ( random.nextDouble() > pAccept ) continue;

			countAcceptedHouseholds++;
			newHouseholds.addHousehold( hh );
			for ( Id id : hh.getMemberIds() ) {
				countAcceptedPersons++;
				newPopulation.addPerson( sc.getPopulation().getPersons().get( id ) );
			}
		}

		log.info( countAcceptedHouseholds+" / "+((MutableScenario) sc).getHouseholds().getHouseholds().size()+
				" households sampled (effective rate "+
				(((double) countAcceptedHouseholds) / ((MutableScenario) sc).getHouseholds().getHouseholds().size()) +" )" );
		log.info( countAcceptedPersons+" / "+sc.getPopulation().getPersons().size()+
				" persons sampled (effective rate "+
				(((double) countAcceptedPersons) / sc.getPopulation().getPersons().size()) );

		new HouseholdsWriterV10( newHouseholds ).writeFile( outHouseholdFile );
		new PopulationWriter( newPopulation , sc.getNetwork() ).write( outPopFile );
	}
}

