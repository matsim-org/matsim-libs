/* *********************************************************************** *
 * project: org.matsim.*
 * OffsetRandomizer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.run;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataImpl;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlReader20;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signals.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signals.model.SignalPlan;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class OffsetRandomizer {

	private static final Logger log = Logger.getLogger(OffsetRandomizer.class);
	public 	static final String SIGNAL_CONTROL_FILENAME = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_control_no_13.xml";
	public 	static final String RANDOM_OFFSET_SIGNAL_CONTROL_FILENAME = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_control_no_13_random_offsets.xml";
	
	public static void main(String[] args) {

		SignalControlData data = new SignalControlDataImpl();
		SignalControlReader20 reader = new SignalControlReader20(data);
		reader.readFile(SIGNAL_CONTROL_FILENAME);
		Random random = MatsimRandom.getLocalInstance();
		
		for (SignalSystemControllerData controlerData : data.getSignalSystemControllerDataBySystemId().values()) {
			SignalPlanData plan = controlerData.getSignalPlanData().get(Id.create("1", SignalPlan.class));
			int cycle = plan.getCycleTime();
			log.debug("cycle: " + cycle);
			double r = random.nextDouble();
			int randomOffset = (int) (r * cycle);
			log.debug("r: " + r + " old offset: " + plan.getOffset() + " new offset: " + randomOffset);
			plan.setOffset(randomOffset);
		}
		SignalControlWriter20 writer = new SignalControlWriter20(data);
		writer.write(RANDOM_OFFSET_SIGNAL_CONTROL_FILENAME);
	}

}
