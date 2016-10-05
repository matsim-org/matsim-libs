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

/**
 * 
 */
package playground.jbischoff.csberlin.evaluation;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.jbischoff.parking.evaluation.ParkedVechiclesVisualiser;
import playground.jbischoff.parking.evaluation.ParkingSearchAndEgressTimeEvaluator;
import playground.jbischoff.parking.evaluation.ParkingSearchEvaluator;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunParkingEvaluation {
public static void main(String[] args) {
	Network network = NetworkUtils.createNetwork();
	new MatsimNetworkReader(network).readFile("../../../shared-svn/projects/bmw_carsharing/data/scenario/network.xml.gz");
	EventsManager events = EventsUtils.createEventsManager();
	ParkingSearchAndEgressTimeEvaluator mierendorffEval = new ParkingSearchAndEgressTimeEvaluator(readLinks("../../../shared-svn/projects/bmw_carsharing/data/gis/mierendorfflinks.txt"),network);	
	ParkingSearchAndEgressTimeEvaluator klausEval = new ParkingSearchAndEgressTimeEvaluator(readLinks("../../../shared-svn/projects/bmw_carsharing/data/gis/klausnerlinks.txt"),network);	
	WalkLegPlotterVIA wlp = new WalkLegPlotterVIA(network,"car");
	WalkLegPlotterVIA wlpFF = new WalkLegPlotterVIA(network,"freefloating");
	ParkedVechiclesVisualiser pvv = new ParkedVechiclesVisualiser(network);
	ParkingSearchEvaluator pwde = new ParkingSearchEvaluator();
	events.addHandler(pwde);
	events.addHandler(pvv);
	events.addHandler(wlp);
	events.addHandler(wlpFF);
	events.addHandler(mierendorffEval);
	events.addHandler(klausEval);
	String dir = "D:/runs-svn/bmw_carsharing/run21/";
	new ParkingSearchEventsReader(events).readFile(dir+"run21.output_events.xml.gz");
	pwde.writeEgressWalkStatistics(dir);
	mierendorffEval.writeStats(dir+"mierendorffParkAndEgressStats.csv");
	mierendorffEval.writeCoordTimeStamps(dir+"/mierendorffParkStamps.csv");
	klausEval.writeStats(dir+"klausnerParkAndEgressStats.csv");
	klausEval.writeCoordTimeStamps(dir+"klausParkStamps.csv");
	wlp.plotWalkLegs(dir+"walkLegs_car.csv");
	wlpFF.plotWalkLegs(dir+"walkLegs_freefloating.csv");
	pvv.finishDay();
	pvv.plotCarPositions(dir+"carPositions.csv");
}

static Set<Id<Link>> readLinks(String filename){
	final Set<Id<Link>> links = new HashSet<>();
	TabularFileParserConfig config = new TabularFileParserConfig();
    config.setDelimiterTags(new String[] {"\t"});
    config.setFileName(filename);
    new TabularFileParser().parse(config, new TabularFileHandler() {
		@Override
		public void startRow(String[] row) {
			links.add(Id.createLinkId(row[0]));
		}
	});

	
	return links;
}


}
