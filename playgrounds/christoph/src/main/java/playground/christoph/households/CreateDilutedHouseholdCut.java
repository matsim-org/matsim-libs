/* *********************************************************************** *
 * project: org.matsim.*
 * CreateDilutedHouseholdCut.java
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

package playground.christoph.households;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.population.filters.PersonIntersectAreaFilter;

/**
 * Creates diluted cut scenario that where only households are removed if no 
 * household member interacts with the observed area.
 * 
 * @author cdobler
 */
public class CreateDilutedHouseholdCut {

//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(CreateDilutedHouseholdCut.class);

	//////////////////////////////////////////////////////////////////////
	// createPopulation()
	//////////////////////////////////////////////////////////////////////

	public static void filterDemand(Config config, String populationOutFile, String householdOutFile, double radius, double xCoord, double yCoord) {

		log.info("MATSim-DB: filterDemand...");

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);

		//////////////////////////////////////////////////////////////////////

		log.info("  calculate area of interest... ");
		final CoordImpl center = new CoordImpl(xCoord, yCoord);
		final Map<Id, Link> areaOfInterest = new HashMap<Id, Link>();
		log.info("    => area of interest (aoi): center=" + center + "; radius=" + radius);

		log.info("    extracting links of the aoi... " + (new Date()));
		for (Link link : scenario.getNetwork().getLinks().values()) {
			final Node from = link.getFromNode();
			final Node to = link.getToNode();
			if ((CoordUtils.calcDistance(from.getCoord(), center) <= radius) || (CoordUtils.calcDistance(to.getCoord(), center) <= radius)) {
				areaOfInterest.put(link.getId(),link);
			}
		}
		log.info("    done. " + (new Date()));
		log.info("    => aoi contains: " + areaOfInterest.size() + " links.");
		log.info("  done. " + (new Date()));

		//////////////////////////////////////////////////////////////////////

		log.info("  initialize intersection filter... ");
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(null, areaOfInterest, scenario.getNetwork());
		filter.setAlternativeAOI(center, radius);
		log.info("  done.");

		//////////////////////////////////////////////////////////////////////
		
		log.info("  filtering population on household level...");
		Set<Household> householdsToRemove = new HashSet<Household>();
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			boolean intersects = false;
			for (Id personId : household.getMemberIds()) {
				if (filter.judge(scenario.getPopulation().getPersons().get(personId))) {
					intersects = true;
					break;
				}
			}
			if (!intersects) householdsToRemove.add(household);
		}
		
		int filteredPersons = 0;
		int filteredHouseholds = 0;
		for (Household household : householdsToRemove) {
			scenario.getHouseholds().getHouseholds().remove(household.getId());
			for (Id personId : household.getMemberIds()) {
				scenario.getPopulation().getPersons().remove(personId);
				filteredPersons++;
			}
			filteredHouseholds++;
		}
		log.info("  done.");
		log.info("    => filtered households: " + filteredHouseholds);
		log.info("    => filtered persons: " + filteredPersons);

		log.info("  removing not selected plans...");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			((PersonImpl) person).removeUnselectedPlans();
		}
		log.info("  done.");
		
		log.info("  writing filtered population to file...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), scenario.getKnowledges()).writeFileV4(populationOutFile);
		log.info("  done.");
		
		log.info("  writing filtered households to file...");
		new HouseholdsWriterV10(scenario.getHouseholds()).writeFile(householdOutFile);
		log.info("  done.");
		
		//////////////////////////////////////////////////////////////////////
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws IOException {

		if (args.length != 6) {
			log.error("Invalid number of arguments. Expected 5 (config file, output file, cut radius, center x coord, center y coord).");
			return;
		}
		
		Gbl.startMeasurement();

		Config config = ConfigUtils.loadConfig(args[0]);
		String populationOutFile = args[1];
		String householdOutFile = args[2];
		double radius = Double.valueOf(args[3]);
		double xCoord = Double.valueOf(args[4]);
		double yCoord = Double.valueOf(args[5]);
		
		filterDemand(config, populationOutFile, householdOutFile, radius, xCoord, yCoord);

		Gbl.printElapsedTime();
	}
}
