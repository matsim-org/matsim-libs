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
package playground.jbischoff.pt.analysis;
/**
 * @author  jbischoff
 *
 */

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;

import playground.vsp.analysis.modules.ptPaxVolumes.PtPaxVolumesAnalyzer;

/**
 *
 */
public class PTAnalysis {
	public static void main(String[] args) {
		PTAnalysis  pta = new PTAnalysis();
		
	}
	private Scenario scenario;
	private String runId = "r12";
	private String outputDirectory = "D:/runs-svn/intermodal/"+runId+"/";
	private String eventFile = "D:/runs-svn/intermodal/"+runId+"/"+runId+".output_events.xml.gz";
	private String scheduleFile = "D:/runs-svn/intermodal/"+runId+"/"+runId+".output_transitSchedule.xml.gz";
	private String vehiclesFile = "D:/runs-svn/intermodal/"+runId+"/"+runId+".output_transitVehicles.xml.gz";
	private String networkFile = "D:/runs-svn/intermodal/"+runId+"/"+runId+".output_network.xml.gz";
	
	/**
	 * 
	 */
	public PTAnalysis() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new TransitScheduleReader(scenario).readFile(scheduleFile);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(vehiclesFile);
		this.rPtPaxVolumes();
	}
	private void rPtPaxVolumes(){
		
			PtPaxVolumesAnalyzer ppv = new PtPaxVolumesAnalyzer(scenario, 30.0*60.0, TransformationFactory.DHDN_GK4);
			EventsManager events = EventsUtils.createEventsManager();
			List<EventHandler> handler = ppv.getEventHandler();
			for(EventHandler eh : handler){
				events.addHandler(eh);
			}
			MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile(eventFile );
			ppv.postProcessData();
			(new File(outputDirectory + "/PtPaxVolumes")).mkdir(); 
			ppv.writeResults(outputDirectory + "/PtPaxVolumes/");
		}
}
