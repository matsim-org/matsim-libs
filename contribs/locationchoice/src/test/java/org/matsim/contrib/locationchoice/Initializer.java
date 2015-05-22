/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestCase;

public class Initializer {

	private Controler controler;

	public void init(MatsimTestCase testCase) {
		// lnk does not work. get path to locationchcoice
		String	path = testCase.getPackageInputDirectory() + "config.xml";
		
		Config config = ConfigUtils.loadConfig(path, new DestinationChoiceConfigGroup() ) ;
		
		//Config config = testCase.loadConfig(path);
				
		this.controler = new Controler(config);
        this.controler.getConfig().controler().setCreateGraphs(false);
        this.controler.getConfig().controler().setWriteEventsInterval(0); // disables events-writing
		this.controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		this.controler.run();
	}

	public Controler getControler() {
		return controler;
	}

}
