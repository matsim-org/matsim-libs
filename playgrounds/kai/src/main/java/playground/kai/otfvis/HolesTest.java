/* *********************************************************************** *
 * project: kai
 * HolesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.kai.otfvis;

import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * @author nagel
 *
 */
public class HolesTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler controler = new Controler( "examples/config/holes-config.xml" ) ;
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controler.addOverridingModule(new OTFVisFileWriterModule());

		controler.run() ;
		
		org.matsim.contrib.otfvis.OTFVis.main( new String[] {"output/holes/ITERS/it.0/0.otfvis.mvi"} ) ;
	}

}
