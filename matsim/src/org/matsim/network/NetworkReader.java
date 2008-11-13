/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReader.java
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

package org.matsim.network;

import java.io.IOException;


/**
 * generic network reader interface
 * 
 * @author balmermi
 */
public interface NetworkReader {
	
	/**
	 * read network from files.
	 * No input file is given, since the number of input files can vary.
	 * 
	 * @throws IOException
	 * @author balmermi
	 */
	void read() throws IOException;
}
