/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wdoering.debugvisualization;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.wdoering.debugvisualization.controller.Console;
import playground.wdoering.debugvisualization.controller.ConsoleImpl;
import playground.wdoering.debugvisualization.controller.Controller;
import playground.wdoering.debugvisualization.controller.XYVxVyEventThread;
import playground.wdoering.oldstufffromgregor.XYVxVyEventsFileReader;


/**
 * Debug Visualization
 *
 * video editing akin exploration of events
 * displaying traces of agents.
 *
 * @author wdoering
 *
 */
public class DebugVisualization {


	public static void main(final String[] args) {

		//console interface for status and debug tracking
		Console console = new ConsoleImpl(false);
		
		Config c = ConfigUtils.createConfig();
		
		c.network().setInputFile(args[1]);
		
		Scenario sc = ScenarioUtils.loadScenario(c);
		//sc.getNetwork().get -> nur über Links (hat from / to nodes (getcoord (get x y)))
		
		EventsManager e = EventsUtils.createEventsManager();
		
		//argument syntax: DebugSim.java eventfile.xml networkfile.xml shapefile.shp liveMode [=true / false / null||else(=false) ]
		if ((args.length > 0) && (!args[0].equals("")))
		{
			//console.println("Initializing Debug Simulation.");
			//Controller controller = new Controller(args[0], args[1], console, 3, true);
			
			String eventFile = args[0];
			String shapeFile = args[2];

			XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(e);
			
			Thread readerThread = new Thread(new XYVxVyEventThread(reader,eventFile), "readerthread");
			
			
			Controller controller = new Controller(e, sc, console, readerThread, shapeFile);
			

		}
		else
		{
			console.println("Too few arguments.");
			System.exit(0);
		}


	}

}
