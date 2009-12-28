/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.cmcf;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

import playground.dgrether.DgPaths;
import playground.dgrether.linkanalysis.TTGraphWriter;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;


/**
 * @author dgrether
 *
 */
public class CMCFEventsAnalyzer {

	private static final String iteration = "550";
	
	private static final String EventsMainRoute = DgPaths.VSPCVSBASE + "runs/run650/it."+iteration+"/"+iteration+".events.txt.gz";

	private static final String EventsAltRoute = DgPaths.VSPCVSBASE + "runs/run651/it."+iteration+"/"+iteration+".events.txt.gz";

	private static final String OUTBASEMain = DgPaths.VSPCVSBASE + "runs/run650/it."+iteration+"/";
	
	private static final String OUTBASEAlt = DgPaths.VSPCVSBASE + "runs/run651/it."+iteration+"/";
	
	private static final String NEWEVENTSALTROUTE = DgPaths.WSBASE + "testdata/output/cmcfNewAltRouteNoReroute/ITERS/it." +
			iteration + "/"+iteration+".events.txt.gz";
	
	private static final String NEWOUTBASEALTROUTE = DgPaths.WSBASE + "testdata/output/cmcfNewAltRouteNoReroute/";
	
	private static final String eventsfile = NEWEVENTSALTROUTE;
	private static final String outbase = NEWOUTBASEALTROUTE;
	
//	private static final String eventsfile = EventsAltRoute;
//	private static final String outbase = OUTBASEAlt;
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EventsManagerImpl events = new EventsManagerImpl();
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);
		
		TTInOutflowEventHandler handler3 = new TTInOutflowEventHandler(new IdImpl("3"), new IdImpl("5"));
		TTInOutflowEventHandler handler4 = new TTInOutflowEventHandler(new IdImpl("4"));
		
		events.addHandler(handler3);
		events.addHandler(handler4);
		
		System.out.println("Reading event-file: "+ eventsfile);
		eventsReader.readFile(eventsfile);
		System.out.println("Processed event-file.");
		

		TTGraphWriter ttWriter = new TTGraphWriter();
		ttWriter.addTTEventHandler(handler3);
		ttWriter.addTTEventHandler(handler4);
		ttWriter.writeTTChart(outbase, Integer.valueOf(iteration));
		
		
//		TTInOutflowGraphWriter.writeInOutFlowChart(handler3, outbase);
//		TTInOutflowGraphWriter.writeTTChart(handler3, outbase);
//		TTInOutflowGraphWriter.writeInOutFlowChart(handler4, outbase);
//		TTInOutflowGraphWriter.writeTTChart(handler4, outbase);
		System.out.println("Graphs written to " + outbase);
	}

}
