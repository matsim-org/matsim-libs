/* *********************************************************************** *
 * project: org.matsim.*
 * Pajek2GML.java
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

/**
 * 
 */
package playground.johannes.snowball;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 *
 */
public class Pajek2GML {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String source = args[0];
		String target = args[1];
		
		Graph g = CustomPajekReader.read(source);
		
		GraphMLFile gmlFile = new GraphMLFile();
		gmlFile.save(g, target);

	}

}
