/* *********************************************************************** *
 * project: org.matsim.*
 * OneSimRunWithConfig.java
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

package playground.david;

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;


public class OneSimRunWithConfig extends Controler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if ( args.length==0 ) {
			Gbl.createConfig(new String[] {"./test/dstrippgen/myconfig.xml"});
		} else {
			Gbl.createConfig(args) ;
		}
				
		final Controler controler = new OneSimRunWithConfig();
		controler.setOverwriteFiles(true) ;
		
		// this comes directly from marcel's master controler
		Runtime run = Runtime.getRuntime();
		run.addShutdownHook( new Thread()
				{
					@Override
					public void run()
					{
						controler.shutdown(true);
					}
				} );

		controler.run(null);
		
	}

}
