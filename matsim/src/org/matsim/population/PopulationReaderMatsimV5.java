/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.population;

import java.util.List;

import org.matsim.basic.v01.BasicPopulationReaderMatsimV5;
import org.matsim.facilities.Facilities;
import org.matsim.interfaces.population.Household;
import org.matsim.network.NetworkLayer;


/**
 * @author dgrether
 *
 */
public class PopulationReaderMatsimV5 extends BasicPopulationReaderMatsimV5 {
	

	public PopulationReaderMatsimV5(final NetworkLayer network, final Population population, List<Household> households, Facilities fac) {
		super();
		super.setPopulationBuilder(new PopulationBuilderImpl(network, population, fac));
		super.setHouseholdBuilder(new HouseholdBuilderImpl(households));
	}

	
	
	
}
