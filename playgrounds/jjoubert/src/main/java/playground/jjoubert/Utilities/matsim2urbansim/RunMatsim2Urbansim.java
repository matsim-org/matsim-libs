/* *********************************************************************** *
 * project: org.matsim.*
 * RunMatsim2Urbansim.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;

public class RunMatsim2Urbansim {
	private final static Logger log = Logger.getLogger(RunMatsim2Urbansim.class);
	private static String root; 
	private static String studyAreaName;
	private static String version;
	
	/**
	 * The following arguments are required:
	 * 
	 * @param args a String-array containing:
	 * <ol>
	 * 	<li> the root folder
	 * 	<li> study area name. Currently allowed values are:
	 * 		<ul>
	 * 			<li> "eThekwini"
	 * 		</ul> 
	 * </ol>
	 */
	public static void main(String[] args) {
		int numberOfArguments = 3;
		if(args.length != numberOfArguments){
			throw new RuntimeException("Incorrect number of arguments provided.");
		} else{
			root = args[0];
			studyAreaName = args[1];
			version = args[2];
		}
		M2UStringbuilder sb = new M2UStringbuilder(root, studyAreaName, version);
		
		// Read the transportation zones. 
		MyZoneReader r = new MyZoneReader(studyAreaName, sb.getShapefile());
		Collection<MyZone> zones = r.getZones();
		
		// Read the network.
		Scenario s = new ScenarioImpl();
		Network n = s.getNetwork();
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		nr.readFile(sb.getEmmeNetworkFilename());
		Map<Id, ? extends Link> m = n.getLinks();
		Link l = m.get(new IdImpl("312"));
		log.info("Completed network reading.");
	
	}
	

}

