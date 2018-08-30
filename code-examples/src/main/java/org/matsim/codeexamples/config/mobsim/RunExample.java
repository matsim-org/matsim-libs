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
package org.matsim.codeexamples.config.mobsim;

import org.matsim.codeexamples.config.RunFromConfigfileExample;

/**
 * Stub version to keep class that is referenced from documentation.  Rather see {@link RunFromConfigfileExample}.
 * 
 * @author nagel
 */
public class RunExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		final String[] args2 = new String[]{"examples/tutorial/config/example1-config.xml"};
		final String[] args2 = new String[]{"scenarios/equil/config-with-mobsim.xml"};
		RunFromConfigfileExample.main( args2);
	}

}
