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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.events.Events;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.trafficlights.data.SignalGroupDefinition;
import org.matsim.trafficlights.data.SignalGroupDefinitionParser;
import org.matsim.trafficlights.data.SignalSystemConfiguration;
import org.matsim.trafficlights.data.SignalSystemConfigurationParser;
import org.xml.sax.SAXException;

import playground.andreas.intersection.tl.SignalSystemControlerImpl;

public class QSim extends QueueSimulation {

	final private static Logger log = Logger.getLogger(QueueLink.class);

	protected static final int INFO_PERIOD = 3600;

	public QSim(Events events, Plans population, NetworkLayer network) {
		super(network, population, events);
		
		this.network = new QueueNetworkLayer(networkLayer, new TrafficLightQueueNetworkFactory());
		
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
		
			
			
			if (node.getNode().getId().toString().equals("99")){
				SignalSystemControlerImpl nodeControler = new SignalSystemControlerImpl(signalSystemConfigurations.get(node.getNode().getId()));
				node.setSignalSystemControler(nodeControler);
				
				for (Iterator iterator = node.getNode().getInLinks().values().iterator(); iterator.hasNext();) {
					Link link = (Link) iterator.next();
					QLink qLink = (QLink) network.getQueueLink(link.getId());
					qLink.reconfigure(signalSystemConfigurations.get(node.getNode().getId()).getSignalGroupDefinitions());
				}
			}
			
						
		}		
	}

	protected void prepareSim() {
		super.prepareSim();
		log.info("prepareSim");
		readSignalSystemControler();
	}

	protected void cleanupSim() {
		log.info("cleanup");
	}

	public void beforeSimStep(final double time) {
		 log.info("before sim step");
	}
	
	

//	/** Do one step of the simulation run. 
//	 * @return true if the simulation needs to continue */
//	@Override
//	public boolean doSimStep(final double time) {
//
//		this.network.moveLinks(time);
//		this.network.moveNodes(time);
//		
//		// Output from David
//		if (time % INFO_PERIOD == 0) {
//			Date endtime = new Date();
//			long diffreal = (endtime.getTime() - this.starttime.getTime()) / 1000;
//			double diffsim = time - SimulationTimer.getSimStartTime();
//
//			log.info("SIMULATION AT " + Time.writeTime(time) + ": #Veh=" + getLiving() + " lost=" + getLost()
//					+ " simT=" + diffsim + "s realT=" + (diffreal) + "s; (s/r): "
//					+ (diffsim / (diffreal + Double.MIN_VALUE)));
//			Gbl.printMemoryUsage();
//		}
//
//		return isLiving() && (this.stopTime >= time);
//	}

	public void afterSimStep(final double time) {
		 log.info("after sim step");
	}

}