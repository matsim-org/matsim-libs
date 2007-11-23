/* *********************************************************************** *
 * project: org.matsim.*
 * UtilityComputer.java
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

package playground.lnicolas.ktiProject;

import org.matsim.plans.Person;

/**
 * Interface for all classes that compute yes and no utilities for a person, based
 * on household information and on the MunicipalityInformation of the municipality
 * the given person lives in (has its home activity location).
 * @author lnicolas
 */
public interface UtilityComputer {

	public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m);
	
	public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m);
	
}
