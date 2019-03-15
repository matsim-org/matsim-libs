/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.utils.meanPaxPerLinkAnalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunSharedTaxiAnalysis {
public static void main(String[] args) {
	String eventsFile = "D:\\Matsim\\Axer\\BSWOB2.0_Scenarios\\output\\20pct_carToDrt_batteryCharge_0C_2nd150_veh_idx0\\20pct_carToDrt_batteryCharge_0C_2nd150_veh_idx0.output_events.xml.gz";
	String outFile ="D:\\\\Matsim\\\\Axer\\\\BSWOB2.0_Scenarios\\\\output\\\\20pct_carToDrt_batteryCharge_0C_2nd150_veh_idx0\\\\20pct_carToDrt_batteryCharge_0C_2nd150_veh_idx0.output_linkOccupancy.csv";
	EventsManager events = EventsUtils.createEventsManager();
	DrtVehicleOccupancyWithLinkOccupancyEvaluator vehicleOccupancyEvaluator = new DrtVehicleOccupancyWithLinkOccupancyEvaluator(0*3600, 30*3600, 6);
	events.addHandler(vehicleOccupancyEvaluator);
	new MatsimEventsReader(events).readFile(eventsFile);
	BufferedWriter bw = IOUtils.getBufferedWriter(outFile);
	try {
		bw.write("LinkId;vehicles;AvOccupancy");
		for (Entry<Id<Link>,DescriptiveStatistics> e : vehicleOccupancyEvaluator.getLinkOccupancy().entrySet()){
			bw.newLine();
			bw.write(e.getKey().toString()+";"+e.getValue().getN()+";"+e.getValue().getMean());
			
		}
		bw.flush();
		bw.close();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}

}
}



