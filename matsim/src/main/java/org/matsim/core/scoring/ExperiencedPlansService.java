
/* *********************************************************************** *
 * project: org.matsim.*
 * ExperiencedPlansService.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.scoring;


import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public interface ExperiencedPlansService {

	void writeExperiencedPlans(String filename);

	IdMap<Person, Plan> getExperiencedPlans();

	void finishIteration();

}
