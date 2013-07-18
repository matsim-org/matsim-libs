/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioShrinker
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.network;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.lanes.LanesConsistencyChecker;
import playground.dgrether.signalsystems.data.consistency.SignalSystemsDataConsistencyChecker;
import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 */
public class NetLanesSignalsShrinker {
	
	private static final Logger log = Logger.getLogger(NetLanesSignalsShrinker.class);
	
	private static final String smallNetworkFilename = "network_small.xml.gz";
	private static final String simplifiedNetworkFilename = "network_small_simplified.xml.gz";
	private static final String simplifiedLanesFilename = "lanes_network_small.xml.gz";

	private Scenario fullScenario;
	private CoordinateReferenceSystem crs;

	private DgSignalsBoundingBox signalsBoundingBox;

	private Map<Id, Id> originalToSimplifiedLinkIdMatching;

	private Network shrinkedNetwork;

	private LaneDefinitions20 shrinkedLanes;

	private SignalsData shrinkedSignals;

	public NetLanesSignalsShrinker(Scenario scenario, CoordinateReferenceSystem crs){
		this.fullScenario = scenario;
		this.crs = crs;
	}
	
	public void shrinkScenario(String outputDirectory, String shapeFileDirectory, double boundingBoxOffset) throws IOException{
		//Some initialization
		Set<Id> signalizedNodes = this.getSignalizedNodeIds(this.fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData(), this.fullScenario.getNetwork());
		DgNetworkUtils.writeNetwork2Shape(fullScenario.getNetwork(), crs, shapeFileDirectory + "network_full");
		
		//create the bounding box
		this.signalsBoundingBox = new DgSignalsBoundingBox(crs);
		Envelope boundingBox = signalsBoundingBox.calculateBoundingBoxForSignals(fullScenario.getNetwork(), 
				fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData(), boundingBoxOffset);
		signalsBoundingBox.writeBoundingBox(shapeFileDirectory);
		
		//Reduce the network size to the bounding box
		DgNetworkShrinker netShrinker = new DgNetworkShrinker();
		netShrinker.setSignalizedNodes(signalizedNodes);
		Network smallNetwork = netShrinker.createSmallNetwork(fullScenario.getNetwork(), boundingBox, crs);
		
		DgNetworkUtils.writeNetwork(smallNetwork, outputDirectory +  smallNetworkFilename);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, crs, shapeFileDirectory + "network_small");
		
		//"clean" the small network
		DgNetworkCleaner cleaner = new DgNetworkCleaner();
		cleaner.cleanNetwork(smallNetwork);
		String smallNetworkClean = outputDirectory + "network_small_clean.xml.gz";
		DgNetworkUtils.writeNetwork(smallNetwork, smallNetworkClean);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, crs, shapeFileDirectory + "network_small_clean");

		
		//run a network simplifier to merge links with same attributes
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS1WAY); //PASS1WAY: 1 in- and 1 outgoing link
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS2WAY); //PASS2WAY: 2 in- and 2 outgoing links
		NetworkLanesSignalsSimplifier nsimply = new NetworkLanesSignalsSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
		nsimply.setSimplifySignalizedNodes(false);
		nsimply.simplifyNetworkLanesAndSignals(smallNetwork, this.fullScenario.getScenarioElement(LaneDefinitions20.class), this.fullScenario.getScenarioElement(SignalsData.class));
		this.originalToSimplifiedLinkIdMatching = nsimply.getOriginalToSimplifiedLinkIdMatching();
		
		this.shrinkedNetwork =  smallNetwork;
		this.shrinkedLanes = this.fullScenario.getScenarioElement(LaneDefinitions20.class);
		this.shrinkedSignals = this.fullScenario.getScenarioElement(SignalsData.class);
		
		LanesConsistencyChecker lanesConsistency = new LanesConsistencyChecker(smallNetwork, shrinkedLanes);
		lanesConsistency.checkConsistency();
		
		SignalSystemsDataConsistencyChecker signalsConsistency = new SignalSystemsDataConsistencyChecker(this.shrinkedNetwork, this.shrinkedLanes, this.shrinkedSignals);
		signalsConsistency.checkConsistency();

		//write shrunk data to disk
		String simplifiedNetworkFile = outputDirectory + simplifiedNetworkFilename;
		DgNetworkUtils.writeNetwork(smallNetwork, simplifiedNetworkFile);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, crs, shapeFileDirectory + "network_small_simplified");

		LaneDefinitionsWriter20 lanesWriter = new LaneDefinitionsWriter20(this.fullScenario.getScenarioElement(LaneDefinitions20.class));
		lanesWriter.write(outputDirectory + simplifiedLanesFilename);
		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter(outputDirectory);
		signalsWriter.writeSignalsData(this.fullScenario.getScenarioElement(SignalsData.class));
		
	}
		
	public DgSignalsBoundingBox getSignalsBoundingBox() {
		return this.signalsBoundingBox;
	}
	
	public Map<Id, Id> getOriginalToSimplifiedLinkIdMatching(){
		return this.originalToSimplifiedLinkIdMatching;
	}
	

	private Set<Id> getSignalizedNodeIds(SignalSystemsData signals, Network network){
		Map<Id, Set<Id>> signalizedNodesPerSystem = DgSignalsUtils.calculateSignalizedNodesPerSystem(signals, network);
		Set<Id> signalizedNodes = new HashSet<Id>();
		for (Set<Id> signalizedNodesOfSystem : signalizedNodesPerSystem.values()){
			signalizedNodes.addAll(signalizedNodesOfSystem);
		}
		return signalizedNodes;
	}

	
	public Network getShrinkedNetwork() {
		return shrinkedNetwork;
	}

	
	public LaneDefinitions20 getShrinkedLanes() {
		return shrinkedLanes;
	}

	
	public SignalsData getShrinkedSignals() {
		return shrinkedSignals;
	}
	
	
}
