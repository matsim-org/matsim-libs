/* *********************************************************************** *
 * project: org.matsim.*
 * CreateLegHistogramImproved
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
package air.analysis.lhi;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;



/**
 * @author dgrether
 *
 */
public class CreateLegHistogramImproved {

	public static void main(String[] args) {
		String baseDirectory = "/media/data/work/repos/";
		String[] runs = {
				 "1811"
				};
		
		for (int i = 0; i < runs.length; i++){
			String rundir = baseDirectory + "runs-svn/run" + runs[i] + "/";
			String eventsFilename = rundir + "ITERS/it.0/" + runs[i] + ".0.events.xml.gz";
			String txtOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved.csv";
			String pngOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved_all.png";
			String pngOutputPt = rundir + "ITERS/it.0/" + runs[i] + ".0.leg_histogram_improved_pt.png";

//			eventsFilename = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.events.xml.gz";
//			txtOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved.csv";
//			pngOutput = "/home/dgrether/data/work/matsim/matsimOutput/flight_model_one_line/ITERS/it.0/0.leg_histogram_improved.png";

			
			EventsManager eventsManager = EventsUtils.createEventsManager();
			LegModeHistogramImproved handler = new LegModeHistogramImproved();
			eventsManager.addHandler(handler);
			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);
			handler.write(txtOutput);
			handler.writeGraphic(pngOutput);
//			handler.writeGraphic(pngOutputPt, "pt");
			
		}
	}

}
