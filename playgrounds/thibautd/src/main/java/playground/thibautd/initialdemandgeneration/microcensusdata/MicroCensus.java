/* *********************************************************************** *
 * project: org.matsim.*
 * Microcensus.java
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
package playground.thibautd.initialdemandgeneration.microcensusdata;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * based on the class of same name from balmermi
 * differences are in the way employement is detected and in the ability to load
 * an arbitrary number of population files.
 */
public class MicroCensus {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(MicroCensus.class);

	private final List<Population> populations;
	private final MzGroupsModule groups;
	private static final String WORK = "w";
	private static final String EDUC = "e";
	private static final String MALE = "m";
	private static final String FEMALE = "f";
	private static final String YES = "yes";
	private static final String NO = "no";

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MicroCensus(
			final MzGroupsModule groups,
			final List<String> popFiles) {
		this.groups = groups;
		log.info( "MicroCensus initialised with groups: "+groups.getDescription() );

		this.populations = new ArrayList<Population>();
		for (String popFile : popFiles) {
			log.info( "loading activity chains from file "+popFile );
			Scenario scen = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			(new MatsimPopulationReader( scen )).parse( popFile );
			populations.add( scen.getPopulation() );
		}

		this.create( populations );
		MatsimRandom.getRandom().nextDouble();
	}

	private final void create(final List<Population> pops) {
		Counter count = new Counter( "MicroCensus: import of activity chain #" );

		int corrWork = 0;
		int corrEduc = 0;
		for (Population pop : pops) {
			for (Person pp : pop.getPersons().values()) {
				count.incCounter();
				PersonImpl p = (PersonImpl) pp;
				int age = p.getAge();
				String sex = p.getSex();
				String lic = p.getLicense();
				// work is defined by employement status rather than plan
				// composition, as employed persons may not work every day
				boolean has_work = p.isEmployed();
				// education is defined by desires
				boolean has_educ = p.getDesires() != null && p.getDesires().getActivityDuration( "e" ) > 0;

				// moreover, correct if no educ or no work is set for plans with educ or work
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity a = (Activity) pe;
						if ( a.getType().equals(WORK) && !has_work ) {
							//log.warn( "found unemployed person with work activities with id "+p.getId()+". Setting employed flag to true." );
							corrWork++;
							has_work = true;
						}
						if ( a.getType().equals(EDUC) && !has_educ ) {
							corrEduc++;
							has_educ = true;
						}
					}
				}

				MzGroup group = groups.getGroup(
						age , sex , lic , has_work , has_educ );
				group.add( pp );
			}
		}
		count.printCounter();
		log.info( corrWork+" plans with work and unemployement were found. Employement set to true." );
		log.info( corrEduc+" plans with educ and uneducation were found. Education set to true." );
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public Person getRandomWeightedMZPerson(
			final int age,
			final String sex,
			final String lic,
			final boolean has_work,
			final boolean has_educ) {
		MzGroup g = groups.getGroup( age , sex , lic , has_work , has_educ );

		return g.size() > 0 ? g.getRandomWeightedPerson() : null;
	}

	public List<Population> getPopulations() {
		return populations;
	}
}
