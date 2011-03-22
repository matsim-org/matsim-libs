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

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.utils.misc.Counter;

public class RemoveFacilitiesFromZH {

	private static String facilitiesInFile = "../../matsim/mysimulations/2kw/facilities/facilities.xml.gz";
	private static String facilitiesOutFile = "../../matsim/mysimulations/2kw/facilities/facilities_without_ZH.xml.gz";
	private static String textFile = "../../matsim/mysimulations/2kw/facilities/Facilities2CutFromMATSim.txt";

	public RemoveFacilitiesFromZH(ActivityFacilitiesImpl facilities, Set<Id> facilitiesToRemove) {
		Counter counter = new Counter("removed facilities: ");
		for (Id id : facilitiesToRemove) {
			Object removed = facilities.getFacilities().remove(id);
			if (removed != null) counter.incCounter();
		}
		counter.printCounter();
	}
	
//	public static void main(String[] args) throws Exception {
//		
//		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		
//		new MatsimFacilitiesReader(scenario).parse(facilitiesInFile);
//		
//		FileInputStream fis = null;
//		InputStreamReader isr = null;
//	    BufferedReader br = null;
//  		
//		fis = new FileInputStream(textFile);
//		isr = new InputStreamReader(fis, charset);
//		br = new BufferedReader(isr);
//		Counter counter = new Counter("removed facilities: ");
//		
//		// skip first Line with the Header
//		br.readLine();
//		 
//		String line;
//		while((line = br.readLine()) != null) {
//			String[] cols = line.split(separator);
//			
//			Id id = scenario.createId(cols[1]);
//			scenario.getActivityFacilities().getFacilities().remove(id);
//			counter.incCounter();
//		}
//		counter.printCounter();
//		
//		br.close();
//		isr.close();
//		fis.close();
//		
//		new FacilitiesWriter(scenario.getActivityFacilities()).write(facilitiesOutFile);
//	}
}
