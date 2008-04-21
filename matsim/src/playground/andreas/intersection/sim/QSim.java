/* *********************************************************************** *
 * project: org.matsim.*
 * ItsumoSim.java
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

package playground.andreas.intersection.sim;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.Simulation;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.plans.Plans;
import org.matsim.trafficlights.data.SignalGroupDefinition;
import org.matsim.trafficlights.data.SignalGroupDefinitionParser;
import org.matsim.trafficlights.data.SignalSystemConfiguration;
import org.matsim.trafficlights.data.SignalSystemConfigurationParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.SAXException;

import playground.andreas.intersection.tl.SignalSystemControlerImpl;

public class QSim extends Simulation {

	final private static Logger log = Logger.getLogger(QueueLink.class);

	protected static final int INFO_PERIOD = 3600;

	private static Events events = null;
	private Plans plans;
	private QNetworkLayer network;
	private Config config;

	public QSim(Events events, Plans population, QNetworkLayer network) {
		super();
		setEvents(events);
		this.plans = population;
		this.network = network;
		this.config = Gbl.getConfig();
	}
	
	private void readSignalSystemControler(){
		
		Map<Id, SignalSystemConfiguration> signalSystemConfigurations = null;
				
		final String signalSystems = "./src/playground/andreas/intersection/signalSystemConfig.xml";
		final String groupDefinitions = "./src/playground/andreas/intersection/signalGroupDefinition.xml";
				
		try {
			List<SignalGroupDefinition> signalGroups = new LinkedList<SignalGroupDefinition>();
			
			SignalGroupDefinitionParser groupParser = new SignalGroupDefinitionParser(signalGroups);
			groupParser.parse(groupDefinitions);
			SignalSystemConfigurationParser signalParser = new SignalSystemConfigurationParser(signalGroups);
			signalParser.parse(signalSystems);
		
			signalSystemConfigurations = signalParser.getSignalSystemConfigurations();
		
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (Iterator iter = network.getNodes().values().iterator(); iter.hasNext();) {
			QNode node = (QNode) iter.next();
		
			
			
			if (node.getId().toString().equals("99")){
				SignalSystemControlerImpl nodeControler = new SignalSystemControlerImpl(signalSystemConfigurations.get(node.getId()));
				node.setSignalSystemControler(nodeControler);
				
				for (Iterator iterator = node.getInLinks().values().iterator(); iterator.hasNext();) {
					QLink qLink = (QLink) iterator.next();
					qLink.reconfigure(signalSystemConfigurations.get(node.getId()).getSignalGroupDefinitions());
				}
			}
			
						
		}		
	}

	protected void prepareSim() {
		log.info("Preparing Sim");

		double startTime = this.config.simulation().getStartTime();
		this.stopTime = this.config.simulation().getEndTime();

		if (startTime == Time.UNDEFINED_TIME) {
			startTime = 0.0;
		}

		if (this.stopTime == Time.UNDEFINED_TIME || this.stopTime == 0) {
			this.stopTime = Double.MAX_VALUE;
		}

		SimulationTimer.setSimStartTime(24 * 3600);
		SimulationTimer.setTime(startTime);

		if (this.plans == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}

		// Put agents in vehicle
		this.plans.addAlgorithm(new QCreateVehicle());
		this.plans.runAlgorithms();
		this.plans.clearAlgorithms();

		// set sim start time to config-value ONLY if this is LATER than the first plans starttime
		SimulationTimer.setSimStartTime(Math.max(startTime, SimulationTimer.getSimStartTime()));
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());
		
		readSignalSystemControler();

	}

	protected void cleanupSim() {
		log.info("cleanup");
	}

	public void beforeSimStep(final double time) {
		// log.info("before sim step");
	}

	/** Do one step of the simulation run. 
	 * @return true if the simulation needs to continue */
	@Override
	public boolean doSimStep(final double time) {

		this.network.moveLinks(time);
		this.network.moveNodes(time);
		
		// Output from David
		if (time % INFO_PERIOD == 0) {
			Date endtime = new Date();
			long diffreal = (endtime.getTime() - this.starttime.getTime()) / 1000;
			double diffsim = time - SimulationTimer.getSimStartTime();

			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + getLiving() + " lost=" + getLost()
					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): "
					+ (diffsim / (diffreal + Double.MIN_VALUE)));
			Gbl.printMemoryUsage();
		}

		return isLiving() && (this.stopTime >= time);
	}

	public void afterSimStep(final double time) {
		// log.info("after sim step");
	}

	/** Need a static call */
	public static Events getEvents() {
		return events;
	}

	/** Needs to be set in a static way */
	private static final void setEvents(final Events events) {
		QSim.events = events;
	}

}