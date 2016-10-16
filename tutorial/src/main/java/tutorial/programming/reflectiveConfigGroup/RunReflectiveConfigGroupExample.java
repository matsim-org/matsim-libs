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
package tutorial.programming.reflectiveConfigGroup;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerUtils;

/**
 * @author nagel
 *
 */
public class RunReflectiveConfigGroupExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig( args[0], new MyConfigGroup() ) ;
		
		// with cast:
//		MyConfigGroup myConfigGroup = (MyConfigGroup) config.getModule( MyConfigGroup.GROUP_NAME ) ;
		
		// or without cast:
		MyConfigGroup myConfigGroup = ConfigUtils.addOrGetModule(config, MyConfigGroup.GROUP_NAME, MyConfigGroup.class ) ;
		
		myConfigGroup.setDoubleField(-99.13) ;
		
		config.checkConsistency(); 
		
		ControlerUtils.checkConfigConsistencyAndWriteToLog(config, "test");
		
	}

}
