/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationV5Converter
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
package playground.dgrether;

import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * @author dgrether
 *
 */
public class PopulationV5Converter {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {

		String net = "examples/equil/network.xml"
		
//		String popv4 = "examples/equil/plans2.xml";
		String popv4 = "test/input/org/matsim/integration/EquilTwoAgentsTest/plans2.xml";
		
//		String popv5 = "examples/equil/plans2v5.xml";
		String popv5 = "test/input/org/matsim/integration/EquilTwoAgentsTest/plans2v5.xml";
		
//		MatsimXMLFormatConverter.convertPopulationV4ToV5(popv4, popv5, net);
		
	}

}
