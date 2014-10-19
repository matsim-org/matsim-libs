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

package playground.anhorni.choiceSetGeneration.choicesetextractors;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.choiceSetGeneration.helper.SpanningTree;
import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;

public abstract class ChoiceSetExtractor {
		
	protected ZHFacilities facilities;
	protected Controler controler;
	private List<ChoiceSet> choiceSets;
	private int tt;
	
	private final static Logger log = Logger.getLogger(ChoiceSetExtractor.class);
	
	public ChoiceSetExtractor(Controler controler, List<ChoiceSet> choiceSets, int tt) {
		this.controler = controler;
		this.choiceSets = choiceSets;
		this.tt = tt;
	} 
	
	
	protected void computeChoiceSets() {

		SpanningTree spanningTree = new SpanningTree(this.controler.getLinkTravelTimes(), this.controler.createTravelDisutilityCalculator());
		String type ="s";
		
		int index = 0;
		List<ChoiceSet> choiceSets2Remove = new Vector<ChoiceSet>();
		
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();										
			this.computeChoiceSet(choiceSet, spanningTree, type, this.controler, this.tt);
			log.info(index + ": Choice set " + choiceSet.getId().toString() + ": " + choiceSet.getFacilities().size() + " alternatives");
			index++;
			
			if (choiceSet.getTravelTimeStartShopEnd(choiceSet.getChosenFacilityId()) > 8 * choiceSet.getTravelTimeBudget()) {	
				choiceSets2Remove.add(choiceSet);			
			}
			
			// remove the trips which end outside of canton ZH:
			/*
			 * change choice set list to TreeMap or similar
			 */
			if (choiceSet.getId().equals(Id.create("8160012", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("58690014", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("30195012", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("31953012", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("55926012", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("58650012", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("55443011", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("44971012", ChoiceSet.class)) ) {
				
				choiceSets2Remove.add(choiceSet);				
			}
			
			// remove trips with a walk TTB >= 7200 s:
			if (choiceSet.getId().equals(Id.create("27242011", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("27898011", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("42444011", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("65064011", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("15359011", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("27691011", ChoiceSet.class)) ||
				choiceSet.getId().equals(Id.create("65679015", ChoiceSet.class))) {
				
				choiceSets2Remove.add(choiceSet);
			}
			
			
		}
		
		Iterator<ChoiceSet> choiceSets2Remove_it = choiceSets2Remove.iterator();
		while (choiceSets2Remove_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets2Remove_it.next();
			this.choiceSets.remove(choiceSet);
			log.info("Removed choice set: " + choiceSet.getId() + " as travel time was implausible or trip ended outside canton ZH");
		}		
	}
		
	protected abstract void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type,
			Controler controler, int tt);
		
	public List<ChoiceSet> getChoiceSets() {
		return choiceSets;
	}

	public void setChoiceSets(List<ChoiceSet> choiceSets) {
		this.choiceSets = choiceSets;
	}

}
