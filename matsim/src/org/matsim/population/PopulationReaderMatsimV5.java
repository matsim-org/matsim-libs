/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import java.util.Stack;

import org.matsim.basic.v01.BasicPopulationReaderMatsimV5;
import org.matsim.basic.v01.PopulationSchemaV5Names;
import org.matsim.facilities.Facilities;
import org.matsim.network.NetworkLayer;
import org.xml.sax.Attributes;

/**
 * @author dgrether
 */
public class PopulationReaderMatsimV5 extends BasicPopulationReaderMatsimV5 {

	public PopulationReaderMatsimV5(final NetworkLayer network, final Population population, List<Household> households, Facilities fac) {
		super();
		super.setPopulationBuilder(new PopulationBuilderImpl(network, population, fac));
		super.setHouseholdBuilder(new HouseholdBuilderImpl(population, households));
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (!PopulationSchemaV5Names.FISCALHOUSEHOLDID.equalsIgnoreCase(name)){
			//do nothing in case of hhId, as it is set by builder, otherwise proceed normally
			super.startTag(name, atts, context);
		}
	}

}
