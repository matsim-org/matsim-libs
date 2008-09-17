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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.velocity.runtime.directive.Foreach;
import org.matsim.analysis.LegHistogram;
import org.matsim.basic.lightsignalsystems.BasicLanesToLinkAssignment;
import org.matsim.basic.lightsignalsystems.BasicLightSignalGroupDefinition;
import org.matsim.basic.lightsignalsystems.BasicLightSignalSystemDefinition;
import org.matsim.basic.lightsignalsystems.BasicLightSignalSystems;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.lightsignalsystems.MatsimLightSignalSystemConfigurationReader;
import org.matsim.lightsignalsystems.MatsimLightSignalSystemsReader;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;
import org.matsim.trafficlights.data.SignalGroupDefinition;
import org.matsim.trafficlights.data.SignalGroupDefinitionParser;
import org.matsim.trafficlights.data.SignalSystemConfiguration;
import org.matsim.trafficlights.data.SignalSystemConfigurationParser;
import org.matsim.trafficlights.data.TrafficLightsManager;
import org.matsim.utils.vis.otfvis.executables.OnTheFlyClientQuadSwing;
import org.matsim.utils.vis.otfvis.gui.PreferencesDialog;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.utils.vis.otfvis.opengl.gui.PreferencesDialog2;
import org.matsim.utils.vis.otfvis.server.OnTheFlyServer;
import org.xml.sax.SAXException;

import playground.andreas.intersection.tl.NewSignalSystemControlerImpl;
import playground.andreas.intersection.tl.SignalSystemControlerImpl;

public class QSim extends QueueSimulation { //OnTheFlyQueueSim

	@SuppressWarnings("unused")
	final private static Logger log = Logger.getLogger(QueueLink.class);

	protected static final int INFO_PERIOD = 3600;

	final String signalSystems;
	final String groupDefinitions;
	final String newLSADef;
	final String newLSADefCfg; 

	protected OnTheFlyServer myOTFServer = null;
	protected boolean useOTF = true;
	protected LegHistogram hist = null;

	public QSim(Events events, Population population, NetworkLayer network, String signalSystems, String groupDefinitions, boolean useOTF, String newLSADef, String newLSADefCfg) {
		super(network, population, events);

		this.network = new QueueNetwork(this.networkLayer, new TrafficLightQueueNetworkFactory());
		this.signalSystems = signalSystems;
		this.newLSADef = newLSADef;
		this.newLSADefCfg = newLSADefCfg;
		this.groupDefinitions = groupDefinitions;
		this.useOTF = useOTF;

//		this.setVehiclePrototye(QVehicle.class);
	}

	private void readSignalSystemControler(){
		
		BasicLightSignalSystems newSignalSystems = new BasicLightSignalSystems();
		MatsimLightSignalSystemsReader lsaReader = new MatsimLightSignalSystemsReader(newSignalSystems);

		List<BasicLightSignalSystemConfiguration> newSignalSystemsConfig = new ArrayList<BasicLightSignalSystemConfiguration>();
	  	MatsimLightSignalSystemConfigurationReader lsaReaderConfig = new MatsimLightSignalSystemConfigurationReader(newSignalSystemsConfig);
		
		lsaReader.readFile(this.newLSADef);
		lsaReaderConfig.readFile(this.newLSADefCfg);
		
		// Create SubNetLinks
		for (BasicLanesToLinkAssignment laneToLink : newSignalSystems.getLanesToLinkAssignments()) {
			QLink qLink = (QLink) this.network.getQueueLink(laneToLink.getLinkId());
			qLink.reconfigure(laneToLink, this.network);
		}
		
		HashMap<Id, NewSignalSystemControlerImpl> sortedLSAControlerMap = new HashMap<Id, NewSignalSystemControlerImpl>();

		// Create a SignalLightControler for every signal system configuration found
		for (BasicLightSignalSystemConfiguration basicLightSignalSystemConfiguration : newSignalSystemsConfig) {
			NewSignalSystemControlerImpl newLSAControler = new NewSignalSystemControlerImpl(basicLightSignalSystemConfiguration);
			sortedLSAControlerMap.put(basicLightSignalSystemConfiguration.getLightSignalSystemId(), newLSAControler);
		}
		
		// Set the defaultCirculationTime for every SignalLightControler
		// depends on the existence of a configuration for every signalsystem specified
		// TODO [an] defaultSyncronizationOffset and defaultInterimTime still ignored
		for (BasicLightSignalSystemDefinition basicLightSignalSystemDefinition : newSignalSystems.getLightSignalSystemDefinitions()) {
			sortedLSAControlerMap.get(basicLightSignalSystemDefinition.getId()).setCirculationTime(basicLightSignalSystemDefinition.getDefaultCirculationTime());
		}
		
		for (BasicLightSignalGroupDefinition basicLightSignalGroupDefinition : newSignalSystems.getLightSignalGroupDefinitions()) {
			basicLightSignalGroupDefinition.setResponsibleLSAControler(sortedLSAControlerMap.get(basicLightSignalGroupDefinition.getLightSignalSystemDefinitionId()));
			QLink qLink = (QLink) this.network.getQueueLink(basicLightSignalGroupDefinition.getLinkRefId());
			qLink.addLightSignalGroupDefinition(basicLightSignalGroupDefinition);
			((QNode) this.network.getNodes().get(qLink.getLink().getToNode().getId())).setIsSignalizedTrue();
		}
		
		
		
//		// halfnewcode
//		
//		// Sort all SignalGroups by SignalSystemControlerId
//		HashMap<Id, List<BasicLightSignalGroupDefinition>> sortedMap = new HashMap<Id, List<BasicLightSignalGroupDefinition>>();
//		for (BasicLightSignalGroupDefinition basicLightSignalGroupDefinition : newSignalSystems.getLightSignalGroupDefinitions()) {
//			List<BasicLightSignalGroupDefinition> sgList = sortedMap.get(basicLightSignalGroupDefinition.getLightSignalSystemDefinitionId());
//			if (sgList == null){
//				sgList = new ArrayList<BasicLightSignalGroupDefinition>();
//				sortedMap.put(basicLightSignalGroupDefinition.getLightSignalSystemDefinitionId(), sgList);
//			}
//			sgList.add(basicLightSignalGroupDefinition);
//		}
//		
//		HashMap<Id, BasicLightSignalSystemDefinition> sortedLightSignalSystems = new HashMap<Id, BasicLightSignalSystemDefinition>();
//		for (BasicLightSignalSystemDefinition basicLightSignalSystemDefinition : newSignalSystems.getLightSignalSystemDefinitions()) {
//			sortedLightSignalSystems.put(basicLightSignalSystemDefinition.getId(), basicLightSignalSystemDefinition);
//		}
//		
//	
//		for (BasicLightSignalSystemConfiguration basicLightSignalSystemConfiguration : newSignalSystemsConfig) {
//			
//			NewSignalSystemControlerImpl newLSAControler = new NewSignalSystemControlerImpl(basicLightSignalSystemConfiguration);
//			
//			newLSAControler.notifyNodes(this.network, sortedMap.get(basicLightSignalSystemConfiguration.getLightSignalSystemId()));
//			newLSAControler.setCirculationTime(sortedLightSignalSystems.get(basicLightSignalSystemConfiguration.getLightSignalSystemId()).getDefaultCirculationTime());
//
//		}
		

		
		// old code below this line

//		Map<Id, SignalSystemConfiguration> signalSystemConfigurations = null;
//
//		try {
//			List<SignalGroupDefinition> signalGroups = new LinkedList<SignalGroupDefinition>();
//
//			SignalGroupDefinitionParser groupParser = new SignalGroupDefinitionParser(signalGroups);
//			groupParser.parse(this.groupDefinitions);
//			SignalSystemConfigurationParser signalParser = new SignalSystemConfigurationParser(signalGroups);
//			signalParser.parse(this.signalSystems);
//
//			signalSystemConfigurations = signalParser.getSignalSystemConfigurations();
//
//			for (SignalSystemConfiguration signalSystemConfiguration : signalSystemConfigurations.values()) {
//
//				TrafficLightsManager trafficLightManager = new TrafficLightsManager(signalSystemConfiguration.getSignalGroupDefinitions(), this.networkLayer);
//
//				QNode qNode = (QNode) this.network.getNodes().get(signalSystemConfiguration.getId());
//				qNode.setSignalSystemControler(new SignalSystemControlerImpl(signalSystemConfiguration));
////
////				for (Iterator<? extends Link> iterator = qNode.getNode().getInLinks().values().iterator(); iterator.hasNext();) {
////					Link link = iterator.next();
////					QLink qLink = (QLink) this.network.getQueueLink(link.getId());
////					qLink.reconfigure(trafficLightManager);
////				}
//			}
//
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ParserConfigurationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	@Override
	protected void prepareSim() {

		if (this.useOTF){
			this.myOTFServer = OnTheFlyServer.createInstance("AName1", this.network, this.plans, events, false);
		}
		super.prepareSim();

		if (this.useOTF){
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

		if(this.signalSystems != null && this.groupDefinitions != null){
			readSignalSystemControler();
		}
	}

	@Override
	protected void cleanupSim() {
		if (this.useOTF){
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
	@Override
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);
		if (this.useOTF){
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