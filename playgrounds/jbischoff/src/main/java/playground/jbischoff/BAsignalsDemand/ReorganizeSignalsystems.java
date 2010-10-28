/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.jbischoff.BAsignalsDemand;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.signalsystems.MatsimSignalSystemsWriter;
import org.matsim.signalsystems.systems.SignalGroupDefinition;
import org.matsim.signalsystems.systems.SignalSystemDefinition;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.signalsystems.systems.SignalSystemsFactory;
import org.matsim.signalsystems.systems.SignalSystemsImpl;

import playground.droeder.DaPaths;
import playground.droeder.gershensonSignals.DenverScenarioGenerator;

/**
 * @author droeder
 *
 */
public class ReorganizeSignalsystems {
	
	
	private static final Logger log = Logger.getLogger(DenverScenarioGenerator.class);

	private static final String INPUT = DaPaths.DGSTUDIES + "denver\\";
	private static final String OUTPUT =DaPaths.DASTUDIES + "denver\\";
	//INPUT
	private static final String NETWORKFILE = INPUT + "networkDenver.xml";
	private static final String LANESINPUTFILE = INPUT + "laneDefinitions.xml";
	private static final String SIGNALSYSTEMINPUTFILE = INPUT + "signalSystems.xml";
	private static final String PLANSINPUTFILE = INPUT + "plans.xml"; 
	
	// OUTPUT
	private static final String NEWSIGNALSYSTEMS = OUTPUT + "signalSystemsByNodes2.xml";
	
	// DEFINITIONS
	
	//<Node, <sgDef>>
	Map<Id, List<SignalGroupDefinition>> nodeGroups = new HashMap<Id, List<SignalGroupDefinition>>(); 
	
	
	private ScenarioImpl loadOldScenario(){
		ScenarioImpl sc = new ScenarioImpl();
		
		Config conf = sc.getConfig();
		
		// set Network and Lanes
		conf.network().setInputFile(NETWORKFILE);
		conf.scenario().setUseLanes(true);
		conf.network().setLaneDefinitionsFile(LANESINPUTFILE);
		
		
		// set plans
		conf.plans().setInputFile(PLANSINPUTFILE);
		
		//set Signalsystems
		conf.scenario().setUseSignalSystems(true);
		conf.signalSystems().setSignalSystemFile(SIGNALSYSTEMINPUTFILE);
		
		//create and write SignalSystemConfig
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(sc);
		loader.loadScenario();
		
		return sc;
	}
	
	private void reorganize(){
		Id[] id = new Id[500];
		int i = 1;
		
		ScenarioImpl oldSc = loadOldScenario();
		calcNodeGroups(oldSc);
		SignalSystems systems = new SignalSystemsImpl();
		SignalSystemsFactory factory = systems.getFactory();
		
		
		
		for (Entry<Id, List<SignalGroupDefinition>> e : this.nodeGroups.entrySet()){
 			id[i] = new IdImpl(i);
			SignalSystemDefinition definition = factory.createSignalSystemDefinition(id[i]);
			systems.addSignalSystemDefinition(definition);
			for (SignalGroupDefinition sd : e.getValue()){
				SignalGroupDefinition[] sgd = new SignalGroupDefinition[5];
				for (int it = 0; it <sd.getLaneIds().size(); it++){
					sgd[it] = factory.createSignalGroupDefinition(sd.getLinkRefId(), new IdImpl(sd.getId().toString() + String.valueOf(it)));
				}
				int it = 0;
				for(Id ii : sd.getLaneIds()){
					sgd[it].addLaneId(ii);
					it++;
				}
				it=0;
				for(Id ii : sd.getToLinkIds()){
					sgd[it].addToLinkId(ii);
					it++;
				}
				it=0;
				for(Id ii : sd.getToLinkIds()){
					sgd[it].setSignalSystemDefinitionId(id[i]);
					systems.addSignalGroupDefinition(sgd[it]);
					it++;
				}
//				SignalGroupDefinition sgd = factory.createSignalGroupDefinition(sd.getLinkRefId(), sd.getId());
//				for(Id ii : sd.getLaneIds()){
//					sgd.addLaneId(ii);
//				}
//				for(Id ii : sd.getToLinkIds()){
//					sgd.addToLinkId(ii);
//				}
//				sgd.setSignalSystemDefinitionId(id[i]);
//				systems.addSignalGroupDefinition(sgd);

			}
			i++;
		}
		
		MatsimSignalSystemsWriter ssWriter = new MatsimSignalSystemsWriter(systems);
		ssWriter.writeFile(NEWSIGNALSYSTEMS);
		
	}
	
	private void calcNodeGroups(ScenarioImpl sc){
		Map<Id, SignalGroupDefinition> groups = sc.getSignalSystems().getSignalGroupDefinitions();

		Network net = sc.getNetwork();
		
		Link mainLink;
		
		for (SignalGroupDefinition sg : groups.values()){
			mainLink = net.getLinks().get(sg.getLinkRefId());
			if (!(nodeGroups.containsKey(mainLink.getToNode().getId()))){
				nodeGroups.put(mainLink.getToNode().getId(), new LinkedList<SignalGroupDefinition>());
				nodeGroups.get(mainLink.getToNode().getId()).add(sg);
			}else{
				nodeGroups.get(mainLink.getToNode().getId()).add(sg);
			}
		}
	}
	
	public static void main(String[] args){
		ReorganizeSignalsystems newSig = new ReorganizeSignalsystems();
		newSig.reorganize();
	}

}
