/* *********************************************************************** *
 * project: org.matsim.*
 * PerformAssignationProcedure.java
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
package playground.thibautd.initialdemandgeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.thibautd.initialdemandgeneration.microcensusdata.MicroCensus;
import playground.thibautd.initialdemandgeneration.microcensusdata.MzGroupsModule;
import playground.thibautd.initialdemandgeneration.modules.PersonAssignActivityChains;
import playground.thibautd.initialdemandgeneration.modules.PersonAssignPrimaryActivities;
import playground.thibautd.initialdemandgeneration.modules.PersonAssignShopLeisureLocations;
import playground.thibautd.initialdemandgeneration.modules.PersonAssignToNetwork;
import playground.thibautd.initialdemandgeneration.modules.PersonRemoveUnhandledModes;
import playground.thibautd.initialdemandgeneration.modules.PersonSetLocationsFromKnowledge;
import playground.thibautd.utils.MoreIOUtils;

/**
 * Executable class which assigns activity chains to agents.
 * It takes a config file as argument, with a special 
 * "Assignation" module.
 *
 * it must have the following fields:
 * <ul>
 * <li> "mzPopulationFile*": the path to the plans corresponding to mz act chains
 * <li> "dayOfWeek": "week", "saturday" or "sunday"
 * <li> "outputDir"
 * </ul>
 *
 * The other modules to be set correspond to the scenario fields (population, network,
 * facilities)
 *
 * This class executes the following operations:
 * <ul>
 * <li> it assigns activity chains to agents, from the MZ population(s)
 * <li> it sets the primary activity locations according to the knowledges
 * <li> it sets the secondary activity locations with a neighborhood search
 * <li> it fills the knowledge with the activity options from the facilities
 * </ul>
 *
 * @author thibautd
 */
public class PerformAssignationProcedure {
	private static final Logger log =
		Logger.getLogger(PerformAssignationProcedure.class);

	public static final String CONF_GROUP = "assignation";
	public static final String POP_FILE_FIELD_REGEXP = "mzPopulationFile.*";
	public static final String DOW_FIELD = "dayOfWeek";
	public static final String OUT_FIELD = "outputDir";

	public static void main( final String[] args ) {
		try{
			String configFile = args[ 0 ];
			MzGroupsModule groups = new MzGroupsModule();
			Config config = ConfigUtils.createConfig( );
			config.addModule( MzGroupsModule.NAME , groups );
			ConfigUtils.loadConfig( config , configFile );
			ScenarioImpl scen = (ScenarioImpl) ScenarioUtils.loadScenario( config );
			Module configGroup = config.getModule( CONF_GROUP );
			String outputDir = configGroup.getValue( OUT_FIELD );
			PersonAssignActivityChains.DayOfWeek dow = getDay( configGroup );

			MoreIOUtils.initOut( outputDir );

			List<String> popFiles = new ArrayList<String>();

			for (Map.Entry<String, String> entry : configGroup.getParams().entrySet()) {
				if (entry.getKey().matches( POP_FILE_FIELD_REGEXP )) {
					popFiles.add( entry.getValue() );
				}
			}

			MicroCensus mz = new MicroCensus( groups , popFiles );

			// construct the routine
			// ---------------------------------------------------------------------
			List<PersonAlgorithm> algos = new ArrayList<PersonAlgorithm>();
			Knowledges knowledges = scen.getKnowledges();
			ActivityFacilities facilities = scen.getActivityFacilities();
			// first assign  act chains
			algos.add( new PersonAssignActivityChains(
						dow,
						mz,
						knowledges));
			// correct the modes (mz contains ride an unknown legs)
			algos.add( new PersonRemoveUnhandledModes() );
			// then, set location of primary activities
			algos.add( new PersonSetLocationsFromKnowledge(
						knowledges,
						facilities) );
			// then, search for close secondary activity locations, and assign them
			algos.add( new PersonAssignShopLeisureLocations(
						facilities,
						dow.toOpenningDay() ) );
			// fill the knowledge (the class is badly named. see javadoc)
			algos.add( new PersonAssignPrimaryActivities(
						knowledges,
						facilities) );
			// finally, make facility / activity link information consistent
			algos.add( new PersonAssignToNetwork(
						scen.getNetwork(),
						facilities) );

			// apply the routine
			// ---------------------------------------------------------------------
			for ( Person person : scen.getPopulation().getPersons().values() ) {
				for (PersonAlgorithm algo : algos) {
					algo.run( person );
				}
			}

			new PlansAnalyse().run(scen.getPopulation());
			for ( Population pop : mz.getPopulations() ) {
				new PlansAnalyse().run(pop);
			}

			(new PopulationWriter(
					  scen.getPopulation(),
					  scen.getNetwork(),
					  scen.getKnowledges())).write(
				  outputDir + "plans-"+getDay( configGroup ) +".xml.gz");

			(new MonoActivityPlansPruner()).run( scen.getPopulation() );

			(new PopulationWriter(
					  scen.getPopulation(),
					  scen.getNetwork(),
					  scen.getKnowledges())).write(
				  outputDir + "plans-"+getDay( configGroup ) +"-wo-mono-act.xml.gz");
		} catch(Exception e) {
			// Log the stack trace, and rethrow the exception.
			// This allows the stack trace to be written to the logFile.
			log.error( "got an uncaught exception", e );
			throw new RuntimeException( e );
		}
	}

	private static PersonAssignActivityChains.DayOfWeek
			getDay( final Module configGroup ) {
		String value = configGroup.getValue( DOW_FIELD );
		return PersonAssignActivityChains.DayOfWeek.valueOf( value );
	}
}

