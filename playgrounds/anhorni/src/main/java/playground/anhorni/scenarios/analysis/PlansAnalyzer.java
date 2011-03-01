/* *********************************************************************** *
 * project: org.matsim.*
 * LCControler.java
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

package playground.anhorni.scenarios.analysis;

import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationImpl;

public class PlansAnalyzer {
	private String path = "";
	private int numberOfCityShoppingLocs = -1;
	private int shopCityLinkIds[];
	
	private int [] visitsCounter;
	private BufferedWriter bufferedWriter = null;
			
	public PlansAnalyzer(String outpath, int numberOfCityShoppingLocs, int [] shopCityLinkIds) {
		this.numberOfCityShoppingLocs = numberOfCityShoppingLocs;
		this.shopCityLinkIds = shopCityLinkIds;
		this.path = outpath;
		this.init();
	}
	
	private void init() {
		this.reset();
		
		try { 		
		    bufferedWriter = new BufferedWriter(new FileWriter(
		    		path + "/input/PLOC/3towns/plans/plansAnalysis.txt")); 
		    bufferedWriter.write("Population\t");
		    for (int i = 0; i < this.numberOfCityShoppingLocs; i++) {
		    	 bufferedWriter.write("loc_" + i + "\t");
		    }
		    bufferedWriter.newLine();
		    
		    
		} catch (IOException ex) {
		    ex.printStackTrace();
		} 
	}
	
	public void finalize() {
		try { 
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException ex) {
		    ex.printStackTrace();
		}
	}
	
	private void reset() {
		this.visitsCounter = new int[numberOfCityShoppingLocs];
    }
	
	private void add2VisitsCounter(Id id) {
		int index = ArrayUtils.indexOf(shopCityLinkIds, Integer.parseInt(id.toString()));
		this.visitsCounter[index]++;
	}
	
	 public void run(PopulationImpl pop, String popId) {
			for (Person p : pop.getPersons().values()) {							
				final List<? extends PlanElement> actslegs = p.getSelectedPlan().getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					ActivityImpl act = (ActivityImpl)actslegs.get(j);					
					if (act.getType().equals("sc")) {
						this.add2VisitsCounter(act.getLinkId());
					}	
				}
			}
			this.writePopulationAnalysis(popId);
	}
	
    private void writePopulationAnalysis(String popId) { 	        
		try { 
			bufferedWriter.append(popId + "\t");
			for (int i = 0; i < this.visitsCounter.length; i++) {
				bufferedWriter.append(this.visitsCounter[i] + "\t");
			}
			bufferedWriter.newLine();			
			bufferedWriter.flush();
			this.reset();
		} catch (IOException ex) {
		    ex.printStackTrace();
		} 
    }
}
