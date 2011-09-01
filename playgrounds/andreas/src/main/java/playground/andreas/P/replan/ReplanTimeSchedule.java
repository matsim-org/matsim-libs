/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P.replan;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.andreas.P.init.CreateInitialTimeSchedule;
import playground.andreas.P.init.PConfigGroup;
import playground.andreas.osmBB.extended.TransitScheduleImpl;
import playground.andreas.utils.pt.TransitScheduleCleaner;

@Deprecated
public class ReplanTimeSchedule {
	
	private TreeMap<Id, SchedulePlans> population = new TreeMap<Id, SchedulePlans>();
	
	public void replan(PConfigGroup pConfig, NetworkImpl net){
		
		String eventsFile = pConfig.getCurrentOutputBase() + "ITERS/it.0/0.events.xml.gz";
		
		TransitSchedule inSchedule = new TransitScheduleImpl(new TransitScheduleFactoryImpl());
		new TransitScheduleReaderV1(inSchedule, net).readFile(pConfig.getCurrentOutputBase() + "transitSchedule.xml");			
		TreeMap<Id, Double> scores = ScorePlans.scorePlans(eventsFile, net);
		
		for (Entry<Id, TransitLine> entry : inSchedule.getTransitLines().entrySet()) {
			if(this.population.get(entry.getKey()) == null){
				this.population.put(entry.getKey(), new SchedulePlans(entry.getKey(), pConfig.getNumberOfPlans()));
			}
			
			this.population.get(entry.getKey()).addTransitPlan(entry.getValue(), scores.get(entry.getKey()));
		}
		
		inSchedule = TransitScheduleCleaner.removeAllLines(inSchedule);
		
		for (SchedulePlans plan : this.population.values()) {
			if(plan.getBestPlan() != null){
				inSchedule.addTransitLine(plan.getBestPlan());
			} else {
				inSchedule.addTransitLine(CreateInitialTimeSchedule.createSingleTransitLine(net, inSchedule, plan.getAgentId()));
			}				
		}	
		new TransitScheduleWriterV1(inSchedule).write(pConfig.getNextOutputBase() + "transitSchedule.xml");
	}

}
