/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.analysis.quick;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author  jbischoff
 *
 */
public class TravelTimeStatistics   {

	public static void main(String[] args) {
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw026.100pct/ITERS/it.150/vw026.100pct.150.events.xml.gz";
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw027.100pct/ITERS/it.180/vw026.100pct.180.events.xml.gz";
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw028.100pct/ITERS/it.210/vw028.100pct.210.events.xml.gz";
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vwo/ITERS/it.20/vwo.20.events.xml.gz";
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vwo1/ITERS/it.50/vwo1.50.events.xml.gz";		
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw029.100pct/ITERS/it.180/vw029.100pct.180.events.xml.gz";
//		String inputFile = "D:/runs-svn/vw_rufbus/vw038/ITERS/it.0/vw038.0.events.xml.gz";
//		String inputFile = "D:/runs-svn/vw_rufbus/vw040/ITERS/it.180/vw040.180.events.xml.gz";
//		String inputFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw042/ITERS/it.0/vw042.0.events.xml.gz";
//		String inputFile = "D:/runs-svn/vw_rufbus/vwTB04/vwTB04.output_events.xml.gz";
//		String inputFile = "D:/runs-svn/vw_rufbus/vw054/vw054.output_events.xml.gz";
//		String inputFile = "D:/runs-svn/vw_rufbus/vw057/vw056.output_events.xml.gz";
		
		
		
		String run = "vw060TBTW15";
//		String folder = "D:/runs-svn/vw_rufbus/" + run + "/";
		String folder = "D:/runs-svn/vw_rufbus/" + run + "/";
		String inputFile = folder + run + ".output_events.xml.gz";
		
		if (args.length>0){
		inputFile=args[0];	
		File f = new File (inputFile);
		folder = f.getParent()+"/";
		}
		System.out.println("Processing Events File: " + inputFile);
		System.out.println("Output will be written to: " + folder);
		

		EventsManager events = EventsUtils.createEventsManager();

		Set<Id<Link>> links = new HashSet<>();
		links.add(Id.createLinkId(57196)); // a39
		links.add(Id.createLinkId(42571)); // L295

		TTEventHandler carTT = new TTEventHandler();
		TTEventHandler allTT = new TTEventHandler();
		TTEventHandler tbTT = new TTEventHandler();
		TTEventHandler ptTT = new TTEventHandler();
		TaxibusRideTimeAnalyser analyser = new TaxibusRideTimeAnalyser();

		carTT.addMode("car");

		allTT.addMode("car");
		allTT.addMode("pt");
		allTT.addMode("taxibus");

		ptTT.addMode("pt");

		tbTT.addMode("taxibus");

		TaxiBusTravelTimesAnalyzer a = new TaxiBusTravelTimesAnalyzer();

		events.addHandler(carTT);
		events.addHandler(ptTT);
		events.addHandler(tbTT);
		events.addHandler(allTT);
		events.addHandler(analyser);
		
		events.addHandler(a);
		new MatsimEventsReader(events).readFile(inputFile);
		System.out.println(inputFile);

		carTT.writeOutput(folder);
		allTT.writeOutput(folder);
		ptTT.writeOutput(folder);
		tbTT.writeOutput(folder);
		analyser.writeOutput(folder);

		a.printOutput();
	}
	

}
