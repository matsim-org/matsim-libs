/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.opensaml.ws.security.ServletRequestX509CredentialAdapter;

/**
 * @author  jbischoff
 *
 */
public class AdjustNetworkCapacities {
	public static void main(String[] args) {
		
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	String basedir = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/";
	new MatsimNetworkReader(scenario).readFile(basedir+"networkptcgt.xml");
	for (Link link : scenario.getNetwork().getLinks().values()){
		if (link.getId().toString().startsWith("pt")) continue;
		if (decideToAdjust(link.getCoord())){
			link.setCapacity(link.getCapacity()*2);
			if (link.getCapacity()<1200) link.setCapacity(1200);
		}else 
		{
//			link.setCapacity(link.getCapacity()*1.1);
		}
		
	}
	new NetworkWriter(scenario.getNetwork()).write(basedir+"networkptcgt.xml");
	
	}


//	static boolean decideToAdjust(Coord coord){
//		if (coord.getX()<593084) return true;
//		else if (coord.getX()>629810) return true;
//		else if (coord.getY()<5785583) return true;
//		else if (coord.getY()>5817600) return true;
//		else return false;
//	} 
	static boolean decideToAdjust(Coord coord){
		if ((coord.getY()>5785522&&coord.getY()<5800201)&&(coord.getX()>597958&&coord.getX()<610209)) return true;
		else return false;
		
	} 
}
