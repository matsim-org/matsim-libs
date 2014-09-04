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

package playground.andreas.utils.pt;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

public class SpandauSchedule {
	
	private static final Logger log = Logger.getLogger(SpandauSchedule.class);
	
	public static void main(String[] args) {
		final String SCHEDULEFILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_basecase.xml.gz";
		final String NETWORKFILE  = "e:/_shared-svn/andreas/paratransit/input/trb_2012/network.final.xml.gz";
		
		final String NO_SPANDAU_SCHEDULE_FILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/noSpandauBusLines.xml.gz";
		final String SPANDAU_BUS_LINES = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/onlySpandauBusLines.xml.gz";
		
		Set<Id> linesToRemove = new TreeSet<Id>();
		linesToRemove.add(new IdImpl("130-B-130"));
		linesToRemove.add(new IdImpl("131-B-131"));
		linesToRemove.add(new IdImpl("134-B-134"));
		linesToRemove.add(new IdImpl("135-B-135"));
		linesToRemove.add(new IdImpl("136-B-136"));
		linesToRemove.add(new IdImpl("234-B-234"));
		linesToRemove.add(new IdImpl("236-B-236"));
		linesToRemove.add(new IdImpl("237-B-237"));
		linesToRemove.add(new IdImpl("334-B-334"));
		linesToRemove.add(new IdImpl("337-B-337"));
		linesToRemove.add(new IdImpl("M32-B-832"));
		linesToRemove.add(new IdImpl("M37-B-837"));
		
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule baseCaseTransitSchedule = builder.createTransitSchedule();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
		new TransitScheduleReaderV1(baseCaseTransitSchedule, network).readFile(SCHEDULEFILE);
	
		TransitSchedule noSpandauTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, linesToRemove);
		new TransitScheduleWriterV1(noSpandauTransitSchedule).write(NO_SPANDAU_SCHEDULE_FILE);
		

		Set<Id> linesToKeep = new TreeSet<Id>();
		for (Id lineId : noSpandauTransitSchedule.getTransitLines().keySet()) {
			linesToKeep.add(lineId);
		}
		
		TransitSchedule spandauBusLinesTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, linesToKeep);
		new TransitScheduleWriterV1(spandauBusLinesTransitSchedule).write(SPANDAU_BUS_LINES);
	
	
	}

}
