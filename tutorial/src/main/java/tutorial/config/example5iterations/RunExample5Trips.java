/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
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

package tutorial.config.example5iterations;

import tutorial.config.RunFromConfigfileExample;


/**
 * runs trip-based iterations (=DTA) and writes events files.  
 * See the config file for configuration details.
 * 
 * Stub version to keep class that is referenced from documentation.  Rather see {@link RunFromConfigfileExample}.
 * 
 * @author nagel
 *
 */
public class RunExample5Trips {
	public static void main(final String[] args) {
		RunFromConfigfileExample.main( new String[]{ "examples/tutorial/config/example5trips-config.xml" } ) ;
	}

}
