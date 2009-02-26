/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationConverterV4ToV5
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
package org.matsim.utils.io;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriterV5;

/**
 * This class provides helper methods to convert the
 * different MATSim XML versions to other versions.
 *
 * @author dgrether
 *
 */
public class MatsimXMLFormatConverter {

	public static void convertPopulationV4ToV5(final String popv4, final String popv5, final String network) throws FileNotFoundException, IOException{
		NetworkLayer net = new NetworkLayer();
		MatsimNetworkReader reader = new MatsimNetworkReader(net);
		reader.readFile(network);

		Population pop = new PopulationImpl(PopulationImpl.NO_STREAMING);
		MatsimPopulationReader popreader = new MatsimPopulationReader(pop, net);
		popreader.readFile(popv4);

		PopulationWriterV5 writer = new PopulationWriterV5(pop);
		writer.writeFile(popv5);
	}

}
