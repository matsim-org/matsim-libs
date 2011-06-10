/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveFacilitiesFromZH.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.energyflows.facilities;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

public class FacilitiesToRemoveFromZH {

//	private String textFile = "../../matsim/mysimulations/2kw/facilities/Facilities2CutFromMATSim.txt";

	private String separator = ";";
	private Charset charset = Charset.forName("ISO-8859-1");
	private Set<Id> facilitiesToRemove;
	
	public FacilitiesToRemoveFromZH(String textFile) throws Exception {
		facilitiesToRemove = new TreeSet<Id>();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;
  		
		fis = new FileInputStream(textFile);
		isr = new InputStreamReader(fis, charset);
		br = new BufferedReader(isr);
		Counter counter = new Counter("parsed facilities to remove: ");
		
		// skip first Line with the Header
		br.readLine();
		 
		String line;
		while((line = br.readLine()) != null) {
			String[] cols = line.split(separator);
			
			Id id = scenario.createId(cols[1].replace("\"", ""));
			facilitiesToRemove.add(id);
			counter.incCounter();
		}
		counter.printCounter();
		
		br.close();
		isr.close();
		fis.close();
	}
	
	public Set<Id> getFacilitiesToRemove() {
		return Collections.unmodifiableSet(this.facilitiesToRemove);
	}
}
