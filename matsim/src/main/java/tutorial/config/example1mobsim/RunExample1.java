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

package tutorial.config.example1mobsim;

import org.matsim.run.Controler;


/**
 * runs a mobsim and writes events output.  See the config file for configuration details.
 * 
 * @author nagel
 *
 */
public class RunExample1 {

	public static void main(final String[] args) {
		String configFile = "examples/tutorial/config/example1-config.xml" ;
		// DO NOT CHANGE THE ABOVE PATH.  It is referenced from the book, and this here tests it.
		
		Controler controler = new Controler( configFile ) ;
		controler.run() ;
	}

}
