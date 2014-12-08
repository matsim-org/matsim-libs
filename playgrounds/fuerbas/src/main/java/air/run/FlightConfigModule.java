/* *********************************************************************** *
 * project: org.matsim.*
 * FlightConfigModule
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.run;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;



/**
 * @author dgrether
 *
 */
public class FlightConfigModule {

	public static final String MODULE_NAME = "flight";
	public static final String DO_REROUTE_STUCKED = "rerouteStuckedPersons";
	public static final String DO_RANDOMIZED_ROUTING = "randomizedTransitTTAndDisutilityRouting";
	private ConfigGroup flightModule;
	
	public FlightConfigModule(Config config) {
		this.flightModule = config.getModule(MODULE_NAME);
	}

	public boolean doRandomizedTTAndDisutilityRouting(){
		if (this.flightModule != null){
			return Boolean.parseBoolean(this.flightModule.getParams().get(DO_RANDOMIZED_ROUTING));
		}
		return false;
	}
	
	public boolean doRerouteStuckedPersons() {
		if (this.flightModule != null){
			return Boolean.parseBoolean(this.flightModule.getParams().get(DO_REROUTE_STUCKED));
		}
		return false;
	}

	

	
	
	
}
