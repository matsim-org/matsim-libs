/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimLaneDefinitionWriter
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
package org.matsim.lanes;

import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.lanes.basic.BasicLaneDefinitions;


/**
 * Writes the lane definitions according to
 * the http://www.matsim.org/files/dtd/laneDefinitions_v*.xsd
 * grammar.
 * @author dgrether
 *
 */
public class MatsimLaneDefinitionsWriter {
	
	private MatsimJaxbXmlWriter writerDelegate;
	 
	/**
	 * Writes the file with the default format for 
	 * LaneDefinitions within MATSim.
	 * @param lanedefs
	 */
	public MatsimLaneDefinitionsWriter(BasicLaneDefinitions lanedefs){
		this.writerDelegate = new LaneDefinitionsWriter11(lanedefs);
	}
	
	
	public void writeFile(String filename){
		this.writerDelegate.writeFile(filename);
	}
	
}
