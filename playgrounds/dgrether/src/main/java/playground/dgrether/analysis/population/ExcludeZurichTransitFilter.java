/* *********************************************************************** *
 * project: org.matsim.*
 * ExcludeZurichTransitFilter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.population;

import org.matsim.api.core.v01.population.Person;

import playground.dgrether.analysis.io.DgAnalysisReaderFilter;


/**
 * @author dgrether
 */
public class ExcludeZurichTransitFilter implements DgAnalysisReaderFilter {

	@Override
	public boolean doAcceptPerson(Person person) {
		int idi = Integer.parseInt(person.getId().toString());
		return (idi <= 1000000000);
	}
}
