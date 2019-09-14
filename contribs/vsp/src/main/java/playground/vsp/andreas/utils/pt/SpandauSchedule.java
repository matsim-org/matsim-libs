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

package playground.vsp.andreas.utils.pt;

import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

public class SpandauSchedule {
	
	private static final Logger log = Logger.getLogger(SpandauSchedule.class);
	
	public static void main(String[] args) {
		final String SCHEDULEFILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/transitSchedule_basecase.xml.gz";
		final String NETWORKFILE  = "e:/_shared-svn/andreas/paratransit/input/trb_2012/network.final.xml.gz";
		
		final String NO_SPANDAU_SCHEDULE_FILE = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/noSpandauBusLines.xml.gz";
		final String SPANDAU_BUS_LINES = "e:/_shared-svn/andreas/paratransit/input/trb_2012/transitSchedules/onlySpandauBusLines.xml.gz";
		
		Set<Id<TransitLine>> linesToRemove = new TreeSet<Id<TransitLine>>();
		linesToRemove.add(Id.create("130-B-130", TransitLine.class));
		linesToRemove.add(Id.create("131-B-131", TransitLine.class));
		linesToRemove.add(Id.create("134-B-134", TransitLine.class));
		linesToRemove.add(Id.create("135-B-135", TransitLine.class));
		linesToRemove.add(Id.create("136-B-136", TransitLine.class));
		linesToRemove.add(Id.create("234-B-234", TransitLine.class));
		linesToRemove.add(Id.create("236-B-236", TransitLine.class));
		linesToRemove.add(Id.create("237-B-237", TransitLine.class));
		linesToRemove.add(Id.create("334-B-334", TransitLine.class));
		linesToRemove.add(Id.create("337-B-337", TransitLine.class));
		linesToRemove.add(Id.create("M32-B-832", TransitLine.class));
		linesToRemove.add(Id.create("M37-B-837", TransitLine.class));
		
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule baseCaseTransitSchedule = builder.createTransitSchedule();

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
//		new TransitScheduleReaderV1(baseCaseTransitSchedule, network).readFile(SCHEDULEFILE);
		new TransitScheduleReaderV1(baseCaseTransitSchedule, new RouteFactories()).readFile(SCHEDULEFILE);
	
		TransitSchedule noSpandauTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, linesToRemove);
		new TransitScheduleWriterV1(noSpandauTransitSchedule).write(NO_SPANDAU_SCHEDULE_FILE);
		

		Set<Id<TransitLine>> linesToKeep = new TreeSet<>();
		for (Id<TransitLine> lineId : noSpandauTransitSchedule.getTransitLines().keySet()) {
			linesToKeep.add(lineId);
		}
		
		TransitSchedule spandauBusLinesTransitSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(baseCaseTransitSchedule, linesToKeep);
		new TransitScheduleWriterV1(spandauBusLinesTransitSchedule).write(SPANDAU_BUS_LINES);
	
	
	}

}
