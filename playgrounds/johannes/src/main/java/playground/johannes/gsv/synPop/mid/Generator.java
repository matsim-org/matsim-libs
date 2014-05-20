/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.mid;

import java.io.IOException;
import java.util.Map;

import playground.johannes.gsv.synPop.FixActivityTimesTask;
import playground.johannes.gsv.synPop.InsertActivitiesTask;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlanTaskComposite;
import playground.johannes.gsv.synPop.RoundTripTask;
import playground.johannes.gsv.synPop.SetActivityTimeTask;
import playground.johannes.gsv.synPop.SetActivityTypeTask;
import playground.johannes.gsv.synPop.SetFirstActivityTypeTask;
import playground.johannes.gsv.synPop.analysis.ActivityChainTask;
import playground.johannes.gsv.synPop.analysis.ActivityLoadTask;
import playground.johannes.gsv.synPop.analysis.LegDistanceTask;

/**
 * @author johannes
 *
 */
public class Generator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String personFile = "/home/johannes/gsv/mid2008/MiD2008_PUF_Personen.txt";
		String legFile = "/home/johannes/gsv/mid2008/MiD2008_PUF_Wege.txt";
		
		TXTReader reader = new TXTReader();
		Map<String, ProxyPerson> persons = reader.read(personFile, legFile);
		
		ProxyPlanTaskComposite composite = new ProxyPlanTaskComposite();
		
		composite.addComponent(new InsertActivitiesTask());
		composite.addComponent(new SetActivityTypeTask());
		composite.addComponent(new SetFirstActivityTypeTask());
//		composite.addComponent(new RoundTripTask());
		composite.addComponent(new SetActivityTimeTask());
		composite.addComponent(new FixActivityTimesTask());
		
		for(ProxyPerson person : persons.values()) {
			composite.apply(person.getPlan());
		}
		
		ActivityChainTask task = new ActivityChainTask();
		task.analyze(persons.values());
		
		ActivityLoadTask taks2 = new ActivityLoadTask();
		taks2.analyze(persons.values());
		
		LegDistanceTask task3 = new LegDistanceTask();
		task3.analyze(persons.values());
		
		System.out.println(String.format("Generated %s persons.", persons.size()));
	}

}
