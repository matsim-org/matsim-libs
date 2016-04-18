/* *********************************************************************** *
 * project: org.matsim.*
 * DgGreenSplitWriter
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
package playground.dgrether.signalsystems.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;


/**
 * @author dgrether
 *
 */
public class DgGreenSplitWriter {

	private static final String SEPARATOR = "\t";

	private DgSignalGreenSplitHandler greenSplitHandler;

	public DgGreenSplitWriter(DgSignalGreenSplitHandler signalGreenSplitHandler) {
		this.greenSplitHandler = signalGreenSplitHandler;
	}
	
	public  static String createHeader(){
		return "Signal System Id" + SEPARATOR + "Signal Group Id" + SEPARATOR + "Signal Group State" + SEPARATOR + "Time sec.";
	}

	public void writeFile(String filename) {
		try {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		String header = createHeader();
		writer.append(header);
		
		for (Id<SignalSystem> ssid : greenSplitHandler.getSystemIdAnalysisDataMap().keySet()) {
			Map<Id<SignalGroup>, DgSignalGroupAnalysisData> signalGroupMap = greenSplitHandler.getSystemIdAnalysisDataMap().get(ssid).getSystemGroupAnalysisDataMap();
			for (Entry<Id<SignalGroup>, DgSignalGroupAnalysisData> entry : signalGroupMap.entrySet()) {
				// logg.info("for signalgroup: "+entry.getKey());
				for (Entry<SignalGroupState, Double> ee : entry.getValue().getStateTimeMap().entrySet()) {
					// logg.info(ee.getKey()+": "+ee.getValue());
					StringBuilder line = new StringBuilder();
					line.append(ssid);
					line.append(SEPARATOR);
					line.append(entry.getKey());
					line.append(SEPARATOR);
					line.append(ee.getKey());
					line.append(SEPARATOR);
					line.append(ee.getValue());
					writer.append(line.toString());
					writer.newLine();
				}
			}
		}				
		writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
