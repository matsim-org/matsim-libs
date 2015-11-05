/* *********************************************************************** *
 * project: org.matsim.*
 * LinkStatsControlerListener
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
package playground.vsp.energy.trafficstate;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


/**
 * @author dgrether
 *
 */
public class TrafficStateControlerListener implements StartupListener, IterationEndsListener, IterationStartsListener {

	private VolumesAnalyzer volumes;
	
	private TravelTimeCalculator ttcalc;
	
	private Scenario scenario;

	public void notifyStartup(StartupEvent event) {
		this.scenario = event.getControler().getScenario();
	}

	
	public void notifyIterationStarts(IterationStartsEvent event) {
		MutableScenario scenario = (MutableScenario) event.getControler().getScenario();
		EventsManager events = event.getControler().getEvents();
		this.volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1, scenario.getNetwork());
		events.addHandler(this.volumes);
		TravelTimeCalculatorConfigGroup ttcalcGroup = new TravelTimeCalculatorConfigGroup();
		ttcalcGroup.setTraveltimeBinSize(3600);
//		this.ttcalc = new TravelTimeCalculator(scenario.getNetwork(), ttcalcGroup);
		this.ttcalc = new TravelTimeCalculator(scenario.getNetwork(), 3600, 24*3600-1, ttcalcGroup);
		events.addHandler(this.ttcalc);

	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		TrafficState ts = this.calculateTrafficState();
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "traffic_state.xml.gz");
		new TrafficStateXmlWriter(ts).writeFile(filename);
	}

	private TrafficState calculateTrafficState() {
		TrafficState ts = new TrafficState();
	  int numSlots = this.ttcalc.getNumSlots();
    int binSize = this.ttcalc.getTimeSlice();
    
    for (Link link : scenario.getNetwork().getLinks().values()){
    	EdgeInfo info = new EdgeInfo(link.getId());
    	ts.addEdgeInfo(info);
    	for (int i = 0; i < numSlots; i++){
    		double time_sec = i * binSize;
    		double tt = this.ttcalc.getLinkTravelTime(link.getId(), time_sec);
    		TimeBin bin = new TimeBin(time_sec, (i+1)*binSize, link.getLength()/tt);
    		info.getTimeBins().add(bin);
    	}
    }
		return ts;
	}


}
