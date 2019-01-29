/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.core.api.internal;

import java.net.URL;

/**
 * There seems to be some tentative decision (https://matsim.atlassian.net/browse/MATSIM-182) to use read(filename).  Interface marking
 * those readers which adhere to this convention.
 * <p></p>
 * --> now decided to use readFile( filename ) 
 * 
 * @author nagel
 *
 */
public interface MatsimReader {

	void readFile( String filename ) ;

	void readURL( URL url ) ;
	
}
