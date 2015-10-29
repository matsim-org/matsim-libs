/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseSacscOccupancy.java
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

package playground.southafrica.projects.iatbr2012;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;

public class AnalyseSacscOccupancy {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AnalyseSacscOccupancy.class.toString(), args);
		
		Map<Id, Integer> shopMapBase = new HashMap<Id, Integer>();
		Map<Id, Integer> leisureMapBase = new HashMap<Id, Integer>();
		Map<Id, Integer> shopMapComparison = new HashMap<Id, Integer>();
		Map<Id, Integer> leisureMapComparison = new HashMap<Id, Integer>();

		/* Read the GLA facility attributes. */
		MutableScenario general = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(general);
		fr.parse(args[0]);
		for(Id id : general.getActivityFacilities().getFacilities().keySet()){
			if(id.toString().startsWith("sacsc") || id.toString().startsWith("osm")){
				shopMapBase.put(id, 0);
				shopMapComparison.put(id, 0);
				leisureMapBase.put(id, 0);
				leisureMapComparison.put(id, 0);
			}
		}
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(attributes);
		oar.parse(args[1]); 
		
		/* BASE CASE */
		Scenario base = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		/* Read the network. */
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(base);
		nr.parse(args[2]);
		/* Read in the plans file of the base case. */
		MatsimPopulationReader pr = new MatsimPopulationReader(base);
		pr.parse(args[3]);
		for(Id id : base.getPopulation().getPersons().keySet()){
			Plan plan = base.getPopulation().getPersons().get(id).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
//					Id actId = new IdImpl( act.getFacilityId().toString().split("_")[1] );
					Id actId = act.getFacilityId();
					if(act.getType().equalsIgnoreCase("s")){
						shopMapBase.put(actId, shopMapBase.get(actId) + 1 );
					} else if(act.getType().equalsIgnoreCase("l")){
						leisureMapBase.put(actId, leisureMapBase.get(actId) + 1 );
					}
				}
			}
		}
		
		/* COMPARATIVE CASE */
		Scenario compare = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		/* Read the network. */
		nr = new NetworkReaderMatsimV1(compare);
		nr.parse(args[2]);
		/* Read in the plans file of the comparative case. */
		pr = new MatsimPopulationReader(compare);
		pr.parse(args[4]);
		for(Id id : compare.getPopulation().getPersons().keySet()){
			Plan plan = compare.getPopulation().getPersons().get(id).getSelectedPlan();
			for(PlanElement pe : plan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
//					Id actId = new IdImpl( act.getFacilityId().toString().split("_")[1] );
					Id actId = act.getFacilityId();
					if(act.getType().equalsIgnoreCase("s")){
						shopMapComparison.put(actId, shopMapBase.get(actId) + 1 );
					} else if(act.getType().equalsIgnoreCase("l")){
						leisureMapComparison.put(actId, leisureMapBase.get(actId) + 1 );
					}
				}
			}
		}
		
		/* Write the comparison to file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(args[5]);
		try {
			bw.write("Id,Cap,Shop0,Shop1,Leisure0,Leisure1");
			bw.newLine();
			for(Id id : general.getActivityFacilities().getFacilities().keySet()){
				if(id.toString().startsWith("sacsc") || id.toString().startsWith("osm")){
					bw.write(id.toString());
					bw.write(",");
					bw.write(getFacilityCapacity((ActivityFacilityImpl) general.getActivityFacilities().getFacilities().get(id) ) );
					bw.write(",");
					bw.write(String.valueOf(shopMapBase.get(id)));
					bw.write(",");
					bw.write(String.valueOf(shopMapComparison.get(id)));
					bw.write(",");
					bw.write(String.valueOf(leisureMapBase.get(id)));
					bw.write(",");
					bw.write(String.valueOf(leisureMapComparison.get(id)));
					bw.newLine();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + args[5]);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + args[5]);
			}
		}
		
		
		Header.printFooter();
	}

	
	private static String getFacilityCapacity(ActivityFacilityImpl facility){
		double capacity = 0.0;
		for(String s : facility.getActivityOptions().keySet()){
			if(s.equalsIgnoreCase("s") || s.equalsIgnoreCase("l") || s.equalsIgnoreCase("t")){
				capacity += facility.getActivityOptions().get(s).getCapacity();				
			}
		}
		return String.format("%.0f", capacity);
		
	}

}

