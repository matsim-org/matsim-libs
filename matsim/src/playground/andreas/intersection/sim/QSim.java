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
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.analysis.LegHistogram;
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
import org.matsim.utils.vis.otfivs.executables.OnTheFlyClientQuadSwing;
import org.matsim.utils.vis.otfivs.gui.PreferencesDialog;
import org.matsim.utils.vis.otfivs.opengl.OnTheFlyClientQuad;
import org.matsim.utils.vis.otfivs.opengl.gui.PreferencesDialog2;
import org.matsim.utils.vis.otfivs.server.OnTheFlyServer;
import org.xml.sax.SAXException;

import playground.andreas.intersection.tl.SignalSystemControlerImpl;

public class QSim extends QueueSimulation { //OnTheFlyQueueSim

	@SuppressWarnings("unused")
	final private static Logger log = Logger.getLogger(QueueLink.class);

	protected static final int INFO_PERIOD = 3600;
	
	final String signalSystems;
	final String groupDefinitions;
	
	protected OnTheFlyServer myOTFServer = null;
	protected Boolean useOTF = true;
	protected LegHistogram hist = null;

	public QSim(Events events, Plans population, NetworkLayer network, String signalSystems, String groupDefinitions, Boolean useOTF) {
		super(network, population, events);
		
		this.network = new QueueNetworkLayer(networkLayer, new TrafficLightQueueNetworkFactory());
		this.signalSystems = signalSystems;
		this.groupDefinitions = groupDefinitions;
		this.useOTF = useOTF;
		
		this.setVehiclePrototye(QVehicle.class);
	}
	
	private void readSignalSystemControler(){
		
		Map<Id, SignalSystemConfiguration> signalSystemConfigurations = null;
						
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
		
		for (SignalSystemConfiguration signalSystemConfiguration : signalSystemConfigurations.values()) {
			
			QNode qNode = (QNode) network.getNodes().get(signalSystemConfiguration.getId());
			qNode.setSignalSystemControler(new SignalSystemControlerImpl(signalSystemConfiguration));
			
			for (Iterator<? extends Link> iterator = qNode.getNode().getInLinks().values().iterator(); iterator.hasNext();) {
				Link link = (Link) iterator.next();
				QLink qLink = (QLink) network.getQueueLink(link.getId());
				qLink.reconfigure(signalSystemConfigurations.get(qNode.getNode().getId()).getSignalGroupDefinitions());
			}
		}
		
	}

	protected void prepareSim() {
		
		if (useOTF){
			this.myOTFServer = OnTheFlyServer.createInstance("AName1", this.network, this.plans, events, false);
		}
		super.prepareSim();
		
		if (useOTF){
			this.hist = new LegHistogram(300);
			events.addHandler(this.hist);
			
			// FOR TESTING ONLY!
			PreferencesDialog.preDialogClass = PreferencesDialog2.class;
			OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019");
			client.start();
			try {
				this.myOTFServer.pause();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		log.info("prepareSim");
		readSignalSystemControler();
	}

	protected void cleanupSim() {
		if (useOTF){
			this.myOTFServer.cleanup();
			this.myOTFServer = null;
		}
		super.cleanupSim();
	}
//
//	public void beforeSimStep(final double time) {
////		 log.info("before sim step");
//	}
//
	public void afterSimStep(final double time) {
		super.afterSimStep(time);
		if (useOTF){
			this.myOTFServer.updateStatus(time);
		}
	}

	public static void runnIt() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		OnTheFlyClientQuadSwing.main(new String []{"rmi:127.0.0.1:4019"});
	}
}