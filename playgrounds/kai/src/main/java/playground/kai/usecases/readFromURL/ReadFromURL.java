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
package playground.kai.usecases.readFromURL;

import java.net.MalformedURLException;
import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class ReadFromURL {

	/**
	 * @param args
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException {

		Network network = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getNetwork() ;
		MatsimNetworkReader reader = new MatsimNetworkReader(network) ;

//		URL abc = new URL("http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/be/brussels/network/belgium_incl_borderArea_clean_simple_epsg31300projection.xml.gz") ;
//		URL abc = new URL("https://raw.githubusercontent.com/matsim-org/matsim/master/matsim/examples/equil/network.xml") ;
		URL abc = new URL("https://raw.githubusercontent.com/matsim-org/matsimExamples/master/countries/za/capetown/2015-10-15_network.xml") ;
		
//		String abc = "../../matsim/examples/equil/network.xml" ;

		reader.parse(abc);
		
		System.out.println("done");
		
	}

}
