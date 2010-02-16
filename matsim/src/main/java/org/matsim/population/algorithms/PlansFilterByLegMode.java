/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterByLegMode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.population.algorithms;

import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;

/**
 * This algorithm filters out all persons having plans with legs with a certain leg mode.
 * There is (since Jan'10) an enum FilterType which is (hopefully) self-explanatory.
 *
* Plans which do not fulfill the filter criteria are removed from a person, Persons with
 * no plans are removed from the population.
 */
public class PlansFilterByLegMode {
	private static final Logger log = Logger.getLogger(PlansFilterByLegMode.class);


	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	private TransportMode legMode;
//	private boolean exclusiveFilter;

	// optimization: instead of doing a String.equals() every time, we do it once and store the result
	private boolean legModeIsCar;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**
	 * There are two modes how this constructor works:
	 *
	 * <li> If <em>exclusive filtering</em> is used,
	 * only plans are kept where persons travel exclusively with the specified leg mode. </li>
	 *
	 * <li> If
	 * the <em>non-exclusive filtering</em> is used, all plans with at least one leg of the
	 * specified leg mode are kept. </li>
	 *
	 * @param legMode
	 * @param exclusiveFilter
	 *
	 * @deprecated use other constructor instead (in my view).  kai, jan'10
	 */
	@Deprecated // use other constructor instead (in my view).  kai, jan'10
	public PlansFilterByLegMode(final TransportMode legMode, final boolean exclusiveFilter) {
		super();
		this.legMode = legMode;
//		this.exclusiveFilter = exclusiveFilter;
		this.filterType = FilterType.keepAllPlansWithMode ;
		if ( exclusiveFilter ) {
			this.filterType = FilterType.keepPlansWithOnlyThisMode ;
		}
		this.legModeIsCar = legMode.equals(TransportMode.car);
	}

	public FilterType filterType ;
	public enum FilterType { keepAllPlansWithMode, removeAllPlansWithMode, keepPlansWithOnlyThisMode }
	public PlansFilterByLegMode( final TransportMode legMode, final FilterType filterType ) {
		super() ;
		this.legMode = legMode ;
		this.filterType = filterType ;
		this.legModeIsCar = legMode.equals(TransportMode.car);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	private static int cnt = 0 ;
	public void run(Population plans) {
		int planCount = 0;
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		String never = "never";

		TreeSet<Id> pid_set = new TreeSet<Id>();	// ids of persons to remove
		Iterator<Id> pid_it = plans.getPersons().keySet().iterator();
		while (pid_it.hasNext()) {
			Id personId = pid_it.next();
			Person person = plans.getPersons().get(personId);

			for (int i=person.getPlans().size()-1; i>=0; i--) {
				Plan plan = person.getPlans().get(i);
				boolean hasSearchedLegMode = false;
				boolean hasOtherLegMode = false;

				for (int j=1; j<plan.getPlanElements().size(); j+=2) {
					LegImpl leg = (LegImpl)plan.getPlanElements().get(j);
					if (leg.getMode().equals(this.legMode)) {
						hasSearchedLegMode = true;
					} else {
						hasOtherLegMode = true;
					}
				}
				if (this.legModeIsCar && never.equals(((PersonImpl) person).getCarAvail())) {
					// person cannot drive car if she has no car. this means, the person was given a lift by someone else
					// --> do not include this person, as we're only interested in the driver
					if ( cnt < 1 ) {
						cnt ++ ;
						log.warn("This method assumes that mode=car without car availability means `in car as passenger'.  Note that this is different "
								+ "from what is often done in transport planning, where `in car as passenger' is a separate mode.");
						log.warn(Gbl.ONLYONCE) ;
					}
					hasSearchedLegMode = false;
				}
				if ( filterType==FilterType.keepAllPlansWithMode || filterType==FilterType.keepPlansWithOnlyThisMode ) {
					if ((!hasSearchedLegMode) || (hasOtherLegMode && this.filterType==FilterType.keepPlansWithOnlyThisMode )) {
						person.getPlans().remove(i);
						i--;	//otherwise, we would skip one plan
						planCount++;
					}
				} else if ( filterType==FilterType.removeAllPlansWithMode ) {
					if ( hasSearchedLegMode ) {
						person.getPlans().remove(i);
						i--;	//otherwise, we would skip one plan
						planCount++;
					}
				} else {
					log.error("should not happen; aborting ...") ;
					System.exit(-1) ;
				}
			}
			if (person.getPlans().isEmpty()) {
				// the person has no plans left. remove the person afterwards (so we do not disrupt the Iterator)
				pid_set.add(personId);
			}
			else {
			}
		}

		// okay, now remove in a 2nd step all persons we do no longer need
		pid_it = pid_set.iterator();
		while (pid_it.hasNext()) {
			Id pid = pid_it.next();
			plans.getPersons().remove(pid);
		}

		System.out.println("    done.");
		System.out.println("Number of plans removed:   " + planCount);
		System.out.println("Number of persons removed: " + pid_set.size());
	}
}
