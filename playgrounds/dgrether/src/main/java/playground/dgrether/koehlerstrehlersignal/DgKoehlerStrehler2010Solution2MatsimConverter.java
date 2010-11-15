/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010Solution2MatsimConverter
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
package playground.dgrether.koehlerstrehlersignal;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgKoehlerStrehler2010Solution2MatsimConverter {

	private static final String in = DgPaths.STUDIESDG + "koehlerStrehler2010/solution_population_100_agents.xml";
	
	
	public DgKoehlerStrehler2010Solution2MatsimConverter(){}
	
	public void convert(){
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DgKoehlerStrehler2010Solution2MatsimConverter().convert();
	}

}
