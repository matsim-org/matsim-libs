/* *********************************************************************** *
 * project: org.matsim.*
 * PtInteracionCounter.java
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

package playground.mmoyo.analysis.tools;

import java.io.File;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.PlanFragmenter;
import java.util.ArrayList;
import java.util.List;

/**
 * counts the number of "ptInteraction" activities in a population file
 */
public class PtInteracionCounter {
	DataLoader dataLoader = new DataLoader();
	PlanFragmenter planFragmenter = new PlanFragmenter();
	
	final String PT_INTERACTION = "pt interaction";
	final String SEP = " "; 
	
	public PtInteracionCounter(final String strPlansDir){
		File plansDir = new File(strPlansDir); 
		//String parentDir = plansDir.getParentFile().getAbsolutePath() + "/";  
		String parentDir = "../playgrounds/mmoyo/output/precalculation/routed3150/";
		List<Data> dataList = new ArrayList<Data>();
		
		for(String populationFile : plansDir.list()){
			Population population1 = dataLoader.readPopulation(parentDir + populationFile);
			System.out.println(populationFile);
			int agentHasPt=0;
			int pt_intNum=0;
			
			//fragment
			Population population = planFragmenter.run(population1);
			for (Person person : population.getPersons().values()){
				boolean hasPt= false;
				
				for (PlanElement pe: person.getSelectedPlan().getPlanElements()){
					if ((pe instanceof Activity)) {
						Activity act = (Activity)pe;
						if (act.getType().equals(PT_INTERACTION)){
							hasPt= true;	
							pt_intNum++;
						}
					}
				}
			
				if (hasPt){
					agentHasPt++;
				}
			}
			Data data = new Data();
			data.setNumAgents(population.getPersons().size());
			data.setCombination(populationFile);
			data.setNumAgentHavingPt(agentHasPt);
			data.setNumPtInteractions(pt_intNum);
			dataList.add(data);
		}

		for (Data data : dataList){
			System.out.println(data.getCombination() + SEP + data.getNumAgents() + SEP + data.getNumAgentHavingPt() + SEP + data.getNumPtInteractions());
		}
//		
	}
	
	class Data{
		String combination;
		int numAgents;
		int numAgentHavingPt;
		int numPtInteractions;
		
		protected int getNumAgents() {
			return numAgents;
		}
		
		protected void setNumAgents(int numAgents) {
			this.numAgents= numAgents;
		}
		
		protected String getCombination() {
			return combination;
		}
		
		protected void setCombination(String combination) {
			this.combination = combination;
		}
	
		protected int getNumAgentHavingPt() {
			return numAgentHavingPt;
		}
		
		protected void setNumAgentHavingPt(int numAgentHavingPt) {
			this.numAgentHavingPt = numAgentHavingPt;
		}
		
		protected int getNumPtInteractions() {
			return numPtInteractions;
		}
	
		protected void setNumPtInteractions(int numPtInteractions) {
			this.numPtInteractions = numPtInteractions;
		}
		
		
	}
	
	public static void main(String[] args) {
		String dir = "../playgrounds/mmoyo/output/precalculation/routed3150";
		new PtInteracionCounter(dir);
	}

}
