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

package playground.wrashid.lib.tools.events;

import java.io.File;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestCase;

public class RemoveAgentsTest extends MatsimTestCase {

	public void testBasic(){
		String inputEventsFile="test/input/playground/wrashid/PSF2/pluggable/0.events.xml";
		
		String outputEventsFile=getOutputDirectory() + "output-events.xml.gz";
		
		HashSet<Id<Person>> agentIds = new HashSet<>();
		
		agentIds.add(Id.create("1", Person.class));
		
		FilterAgents.removeAgents(agentIds, inputEventsFile, outputEventsFile);
		
		File inputFile = new File(inputEventsFile);
		File outputFile = new File(outputEventsFile);
		
		assertTrue("filtered output cannot be larger than input",inputFile.length()>outputFile.length());
		assertTrue("outputFile is empty",outputFile.length()>0);
	}
	
}
