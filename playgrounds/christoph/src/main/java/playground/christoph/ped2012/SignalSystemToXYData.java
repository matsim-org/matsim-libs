/* *********************************************************************** *
 * project: matsim
 * SignalSystemToXYData.java
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

package playground.christoph.ped2012;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.core.mobsim.qsim.qnetsimengine.SignalGroupState;

/*
 * Creates xy-data from an events file for signal systems.
 */
public class SignalSystemToXYData implements BasicEventHandler {

	private static final String newLine = "\n";
	private static final String separator = "\t";

	private final Network network;
	private final SignalsData signalsData;
	private BufferedWriter bufferedWriter;
	
	public static void main(String[] args) {
//		String inputEventsFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
//		String inputNetworkFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/input/network.xml.gz";
//		String inputSignalSystemsFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/input/signal_systems.xml";
//		String outputXYFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_signal_systems.xy.gz";
	
		String inputEventsFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String inputNetworkFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input/network.xml";
		String inputSignalSystemsFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input/signal_systems.xml";
		String outputXYFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_signal_systems.xy.gz";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputNetworkFile);
		config.scenario().setUseSignalSystems(true);
		config.signalSystems().setSignalSystemFile(inputSignalSystemsFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		new SignalSystemToXYData(scenario.getNetwork(), signalsData).createXYData(inputEventsFile, outputXYFile);
	}
	
	public SignalSystemToXYData(Network network, SignalsData signalsData) {
		this.network = network;
		this.signalsData = signalsData;
	}

	public void createXYData(String inputEventsFile, String outputXYFile) {
		
		try {
			bufferedWriter = IOUtils.getBufferedWriter(outputXYFile);
			writeHeader();
			
			EventsManager eventsManager = EventsUtils.createEventsManager();
			MatsimEventsReader eventReader = new MatsimEventsReader(eventsManager);
						
			eventsManager.addHandler(this);
			eventReader.readFile(inputEventsFile);

			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleEvent(Event event) {
		
		if (event instanceof SignalGroupStateChangedEvent) {
			SignalGroupStateChangedEvent signalEvent = (SignalGroupStateChangedEvent) event;
			
			double time = signalEvent.getTime();
			Id signalGroupId = signalEvent.getSignalGroupId();
			Id signalSystemId = signalEvent.getSignalSystemId();
			SignalGroupState signalGroupState = signalEvent.getNewState();
			
			SignalSystemData ssd = signalsData.getSignalSystemsData().getSignalSystemData().get(signalSystemId);
			
			for (SignalData signalData : ssd.getSignalData().values()) {
				Id linkId = signalData.getLinkId();
				Link link = network.getLinks().get(linkId);
				Coord coord = link.getToNode().getCoord();
				createXYData(time, signalGroupId, signalSystemId, signalGroupState, coord.getX(), coord.getY());
			}
		}
	}
	
	private void writeHeader() {
		try {
			bufferedWriter.write("time");
			bufferedWriter.write(separator);
			bufferedWriter.write("signalGroupId");
			bufferedWriter.write(separator);
			bufferedWriter.write("signalSystemId");
			bufferedWriter.write(separator);
			bufferedWriter.write("signalGroupState");
			bufferedWriter.write(separator);
			bufferedWriter.write("x");
			bufferedWriter.write(separator);
			bufferedWriter.write("y");
			bufferedWriter.write(newLine);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void createXYData(double time, Id signalGroupId, Id signalSystemId, 
			SignalGroupState signalGroupState, double x, double y) {
		try {			
			bufferedWriter.write(String.valueOf(time));	// time
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(signalGroupId.toString()));	// signalGroupId
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(signalSystemId.toString())); // signalSystemId
			bufferedWriter.write(separator);
			bufferedWriter.write(signalGroupState.toString());	// signalGroupState
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(x));	// x
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(y));	// y
			bufferedWriter.write(newLine);			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void reset(int iteration) {
	}
}
