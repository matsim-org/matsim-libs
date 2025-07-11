/* *********************************************************************** *
 * project: org.matsim.*																															*
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

package org.matsim.api.core.v01;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.Lanes;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;


/**
 *
 * The scenario is the entry point to MATSim
 * scenarios. An implementation of Scenario
 * has to provide implementations
 * for the different return types, e.g. Network,
 * Facilities or Population.
 *
 * <h3> Design aspects </h3>
 * <p>
 * Scenario (and in particular {@link MutableScenario}) now accepts arbitrary implementations of the contributing containers.  As stated
 * below, in some sense the whole scenario container is no longer necessary with the injection framework.  The containers themselves
 * should allow replaceable implementations, see {@link MatsimToplevelContainer}.
 * </p>
 * <h3> History </h3>
 * <p>
 * Originally, MATSim provided "global" variables such as access to the network with public static variables.
 * </p><p>
 * This was changed around 2010 towards an approach where everything was moved around by non-static references.  The Scenario
 * container was thus introduces to contain most of the objects that were public static before, so that reaching around one reference
 * would be enough for most purposes.  Reasons for that change include(d):<ul>
 * <li> Some people wanted to be able to have, say, two networks and/or two populations simultaneously in the code.  For example in order to compare them.
 * This was quite difficult with code that used public static variables.  (Clearly, one could put the desired scenario or network to the public
 * static reference, run the method, and then change it back.  Yet, ...)
 * <li> We never noticed it a lot since consistent regression testing was introduced <i> after </i> this change.  Yet when I introduce regression
 * tests for other software that uses public static variables excessively, I have to use a mode that constructs a new JVM every time a test
 * is started.  This works for integration tests since they use a fair amount of time anyways, but presumably makes unit testing much slower.
 * <li> As an argument going into the same direction: Without global static variables, a class or method has access to what is given into
 * it, but not more.  With global static variables, any class or method has access to everything that can be accessed that way.  This means
 * that one can never rely on a class or method to not change more than is visible from the outside, making reliably encapsulated modules
 * close to impossible.
 * </ul>
 * </p><p>
 * Around 2015, this was superseded by the (guice) injection approach.  Since with that it is possible to get the elements of the scenario
 * injected separately, the scenario container essentially does not seem to be necessary any more.  In retrospect, it might have been easier
 * to move from the public static approach to the injection approach right away, but we did not know this at that time.
 * </p>
 *
 * @author dgrether
 * @author (of documentation) Kai Nagel
 */
public interface Scenario {

	Network getNetwork();

	Population getPopulation();

	TransitSchedule getTransitSchedule();

	Config getConfig();

	/**
	 * Adds the given object to the scenario, such it can be
	 * retrieved with {@link #getScenarioElement(String)} using
	 * the name given here as a key.
	 * <br><br>
	 * Design issues:<ul>
	 * <li> This is currently set to deprecated, since the guice injection approach achieves something similar.  However,
	 * at this point injection is only possible after {@link Controler} was started, which stands in the way of <i>first</i> constructing
	 * the scenario and only <i>then</i> starting {@link Controler}.  So maybe we should un-deprecate this method.  Kai/Theresa, sep'16
	 * Un-deprecated for the time being.   Kai, dec'16
	 * </ul>
	 *
	 * @param name the name to which the object should be associated
	 * @param o the object. <code>null</code> is not allowed.
	 *
	 * @throws NullPointerException if the object is null
	 * @throws IllegalStateException if there is already an object
	 * associated to this name.
	 */
	void addScenarioElement(String name, Object o);

	/**
	 * See the "Design issues" under {@link Scenario#addScenarioElement(String, Object)}.
	 *
	 * @param name the name of the element to get
	 * @return the object associated with that name, or null if none is associated
	 */
	Object getScenarioElement(String name);

	ActivityFacilities getActivityFacilities();

	Vehicles getTransitVehicles();

	Vehicles getVehicles();

	Households getHouseholds();

	Lanes getLanes();

}
