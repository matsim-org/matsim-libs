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
package playground.agarwalamit.congestionPricing.analysis;

import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.ikaddoura.internalizationCar.MarginalCongestionEvent;


/**
 * @author amit
 */

public class CompareCongestionEvents  {

	private String eventsFile_v3 = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV3/ITERS/it.1000/1000.events.xml.gz";
	private String eventsFile_v4 = "/Users/amit/Documents/repos/runs-svn/siouxFalls/run203/implV4/ITERS/it.1000/1000.events.xml.gz";

	private List<MarginalCongestionEvent>  getCongestionEvents (String eventsFile){
		
		CongestionEventHandler ceh = new CongestionEventHandler();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		
		eventsManager.addHandler(ceh);
		reader.readFile(eventsFile);

		return ceh.getCongestionEventsAsList();
	}

	public static void main(String[] args) {
		new CompareCongestionEvents().run();
	} 

	private void run(){

		int wrongAgentsCount = 0;
		double wronglyChargedDelays = 0;

		int unchargedAgentsCount =0;
		double unchargedDelays =0;

		List<MarginalCongestionEvent> eventsImpl3 = getCongestionEvents(eventsFile_v3);
		List<MarginalCongestionEvent> eventsImpl4 = getCongestionEvents(eventsFile_v4);

		for(MarginalCongestionEvent e3 : eventsImpl3){
			if(eventsImpl4.contains(e3)){
				//everything is fine.
				eventsImpl4.remove(e3);
			} else {
				wrongAgentsCount++;
				wronglyChargedDelays += e3.getDelay();
			}
		}

		unchargedAgentsCount = eventsImpl4.size();

		for(MarginalCongestionEvent e4 : eventsImpl4){
			unchargedDelays += e4.getDelay();
		}

		System.out.println(wrongAgentsCount+ " number of persons are wrongly charged and total wrongly charged delays in hr are "+ wronglyChargedDelays/3600);
		System.out.println(unchargedAgentsCount+ " number of persons are not charged and total uncharged delays in hr are "+ unchargedDelays/3600);
	}
}
