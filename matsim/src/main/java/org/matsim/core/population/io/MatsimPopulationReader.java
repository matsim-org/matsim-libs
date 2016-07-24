/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderI.java
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

package org.matsim.core.population.io;

import org.matsim.core.api.internal.MatsimReader;


public interface MatsimPopulationReader extends MatsimReader {
	// yyyyyy I think that this interface can go.  kai, jul'16
	
	/**
	 * read plans from the specified file.
	 * 
	 * @param filename name of the file to read plans from.
	 */
	@Override
	void readFile(String filename);

}
