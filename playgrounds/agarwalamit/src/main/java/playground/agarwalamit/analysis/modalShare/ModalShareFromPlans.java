/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.modalShare;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class ModalShareFromPlans implements ModalShare{

	private final Population pop ;
	private SortedMap<String, Integer> mode2numberOflegs = new TreeMap<>();
	private SortedMap<String, Double> mode2PctOflegs = new TreeMap<>();

	public ModalShareFromPlans (final String plansFile) {
		this.pop = LoadMyScenarios.loadScenarioFromPlans(plansFile).getPopulation();
	}

	public ModalShareFromPlans (final Population pop) {
		this.pop = pop;
	}

	public void run() {
		// first store used modes 
		for(Person person : pop.getPersons().values()){
			PlanImpl plan = (PlanImpl) person.getSelectedPlan();
			List<PlanElement> planElements = plan.getPlanElements();
			for(PlanElement pe : planElements){
				if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					String legMode = leg.getMode();
					if(!this.mode2numberOflegs.containsKey(legMode)){
						this.mode2numberOflegs.put(legMode, 0);
					}
				}
			}
		}
		// now store number of legs
		for(String mode : mode2numberOflegs.keySet()){
			int noOfLegs = 0;
			for(Person person : pop.getPersons().values()){
				PlanImpl plan = (PlanImpl) person.getSelectedPlan();
				List<PlanElement> planElements = plan.getPlanElements();
				for(PlanElement pe : planElements){
					if(pe instanceof Leg){
						Leg leg = (Leg) pe;
						String legMode = leg.getMode();
						if(legMode.equals(mode)){
							noOfLegs ++;
						}

					}
				}
			}
			this.mode2numberOflegs.put(mode, noOfLegs);
		}
		
		this.mode2PctOflegs = MapUtils.getPercentShare(this.mode2numberOflegs);
	}

	@Override
	public SortedSet<String> getUsedModes() {
		return new TreeSet<>(this.mode2numberOflegs.keySet());
	}

	@Override
	public SortedMap<String, Integer> getModeToNumberOfLegs() {
		return this.mode2numberOflegs;
	}

	@Override
	public SortedMap<String, Double> getModeToPercentOfLegs() {
		return this.mode2PctOflegs;
	}
	
	@Override
	public void writeResults(String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			for(String str:mode2numberOflegs.keySet()){
				writer.write(str+"\t");
			}
			writer.write("total \t");
			writer.newLine();
			
			for(String str:mode2numberOflegs.keySet()){ // write Absolute No Of Legs
				writer.write(mode2numberOflegs.get(str)+"\t");
			}
			writer.write(MapUtils.intValueSum(mode2numberOflegs)+"\t");
			writer.newLine();
			
			for(String str:mode2PctOflegs.keySet()){ // write percentage no of legs
				writer.write(mode2PctOflegs.get(str)+"\t");
			}
			writer.write(MapUtils.doubleValueSum(mode2PctOflegs)+"\t");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data can not be written to file. Reason - "+e);
		}
	}
}