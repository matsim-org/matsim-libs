/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAssignActivityChains.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * On a great extend based on the class of the same name in playground.balmermi.
 * <br>
 * Modifications were made to take into account the specific characteristics of
 * Saturday and Sunday on the work and education side.
 */
public class PersonAssignActivityChains extends AbstractPersonAlgorithm { 

	public static enum DayOfWeek { week , saturday , sunday };
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignActivityChains.class);

	private final MicroCensus microcensus;

	private final Knowledges knowledges;
	private final DayOfWeek day;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignActivityChains(
			final DayOfWeek day,
			final MicroCensus microcensus,
			final Knowledges knowledges) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.microcensus = microcensus;
		this.knowledges = knowledges;
		this.day = day;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person p) {
		PersonImpl person = (PersonImpl) p;
		boolean has_work = hasWork( person );
		boolean has_educ = hasEducation( person );

		Person mz_p = microcensus.getRandomWeightedMZPerson(person.getAge(),person.getSex(),person.getLicense(), has_work, has_educ);
		if (mz_p == null) {
			log.warn("pid="+person.getId()+": Person does not belong to a micro census group!");
			mz_p = microcensus.getRandomWeightedMZPerson(person.getAge(),"f",person.getLicense(), has_work, has_educ);
			log.warn("=> Assigning same demographics except that person is handled as a female. NOTE: Works only for CH-Microcensus 2005.");
			if (mz_p == null) {
				Gbl.errorMsg("In CH-Microcensus 2005: That should not happen!");
			}
		}
		person.addPlan(mz_p.getSelectedPlan());
		person.setSelectedPlan(mz_p.getSelectedPlan());
	}

	private boolean hasWork(final Person p) {
		KnowledgeImpl knowledge =
			knowledges.getKnowledgesByPersonId().get(p.getId());

		switch (day) {
			case week: 
				return !knowledge.getActivities(CAtts.ACT_W2).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_W3).isEmpty();
			case saturday:
				return !knowledge.getActivities(CAtts.ACT_W2).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_W3).isEmpty();
			case sunday:
				return !knowledge.getActivities(CAtts.ACT_W2).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_W3).isEmpty();
			default:
				throw new RuntimeException( "error in detecting the day" );
		}
	}

	private boolean hasEducation(final Person p) {
		KnowledgeImpl knowledge =
			knowledges.getKnowledgesByPersonId().get(p.getId());

		switch (day) {
			case week: 
				return !knowledge.getActivities(CAtts.ACT_EKIGA).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_EPRIM).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_ESECO).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_EHIGH).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_EOTHR).isEmpty();
			case saturday:
				return !knowledge.getActivities(CAtts.ACT_EKIGA).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_EPRIM).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_ESECO).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_EHIGH).isEmpty() ||
					!knowledge.getActivities(CAtts.ACT_EOTHR).isEmpty();
			case sunday:
				// only for "other" types
				return !knowledge.getActivities(CAtts.ACT_EOTHR).isEmpty();
			default:
				throw new RuntimeException( "error in detecting the day" );
		}
	}
}
