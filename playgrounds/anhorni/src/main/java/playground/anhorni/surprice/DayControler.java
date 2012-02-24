/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;

public class DayControler extends Controler {
	
	public DayControler(final ScenarioImpl scenario) {
		super(scenario);	
		super.setOverwriteFiles(true);
	} 
	
	public DayControler(final Config config) {
		super(config);	
		super.setOverwriteFiles(true);
	} 
	
	public DayControler(final String configFile) {
		super(configFile);	
		super.setOverwriteFiles(true);
	}
}
