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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.lanes.LanesConsistencyChecker;
import playground.dgrether.signalsystems.data.consistency.SignalSystemsDataConsistencyChecker;
import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 * @author tthunig
 */
public class NetLanesSignalsShrinker {
	
	private static final Logger log = Logger.getLogger(NetLanesSignalsShrinker.class);
	
	private static final String bbNetworkFilename = "network_bb_allLinks.xml.gz";
	private static final String smallNetworkFilename = "network_small.xml.gz";
	private static final String simplifiedNetworkFilename = "network_small_simplified.xml.gz";
	private static final String simplifiedLanesFilename = "lanes_network_small.xml.gz";

	private Scenario fullScenario;
	private CoordinateReferenceSystem crs;

	private DgSignalsBoundingBox cuttingBoundingBox;
	
	private Map<Id<Link>, Id<Link>> originalToSimplifiedLinkIdMatching;

	private Network shrinkedNetwork;

	private Lanes shrinkedLanes;

	private SignalsData shrinkedSignals;

	public NetLanesSignalsShrinker(Scenario scenario, CoordinateReferenceSystem crs){
		this.fullScenario = scenario;
		this.crs = crs;
	}
	
	/**
	 * shrink the scenario: reduce the network size to the bounding box around the signalized nodes given by the cuttingBoundingBoxOffset,
	 * delete all small interior edges (freespeed <= 10 m/s) that are not on a shortest path (according to travel time) between signalized nodes,
	 * clean and simplify the resulting small network.
	 * 
	 * @param outputDirectory
	 * @param shapeFileDirectory
	 * @param cuttingBoundingBoxOffset
	 * @param freeSpeedFilter the minimal free speed value for the interior link filter in m/s
	 * @param useFreeSpeedTravelTime a flag for dijkstras cost function:
	 * if true, dijkstra will use the free speed travel time, if false, dijkstra will use the travel distance as cost function 
	 * @param simplifyNetwork use network simplifier if true
	 * @throws IOException
	 */
	public void shrinkScenario(String outputDirectory, String shapeFileDirectory, double cuttingBoundingBoxOffset, double freeSpeedFilter, 
			boolean useFreeSpeedTravelTime, double maximalLinkLength, boolean simplifyNetwork) throws IOException{
		
		//Some initialization
		Set<Id<Node>> signalizedNodes = this.getSignalizedNodeIds(((SignalsData) this.fullScenario.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData(), this.fullScenario.getNetwork());
		DgNetworkUtils.writeNetwork2Shape(fullScenario.getNetwork(), crs, shapeFileDirectory + "network_full");
		
		// create the boundary envelope
		this.cuttingBoundingBox = new DgSignalsBoundingBox(crs);
		Envelope cuttingBoundingBoxEnvelope = cuttingBoundingBox.calculateBoundingBoxForSignals(fullScenario.getNetwork(), 
				((SignalsData) fullScenario.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalSystemsData(), cuttingBoundingBoxOffset);
		cuttingBoundingBox.writeBoundingBox(shapeFileDirectory + "cutting_");
		
		// reduce the network size to the bounding box and filter interior links
		DgNetworkShrinker netShrinker = new DgNetworkShrinker();
		netShrinker.setSignalizedNodes(signalizedNodes);
		
		Network bbNetwork = netShrinker.filterLinksOutsideEnvelope(fullScenario.getNetwork(), cuttingBoundingBoxEnvelope);
		DgNetworkUtils.writeNetwork(bbNetwork, outputDirectory +  bbNetworkFilename);
		DgNetworkUtils.writeNetwork2Shape(bbNetwork, crs, shapeFileDirectory + "network_bb_allLinks");
		
		Network smallNetwork = netShrinker.filterInteriorLinks(bbNetwork, freeSpeedFilter, useFreeSpeedTravelTime);
		DgNetworkUtils.writeNetwork(smallNetwork, outputDirectory +  smallNetworkFilename);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, crs, shapeFileDirectory + "network_small");
		
		// "clean" the small network
		DgNetworkCleaner cleaner = new DgNetworkCleaner();
		cleaner.cleanNetwork(smallNetwork);
		String smallNetworkClean = outputDirectory + "network_small_clean.xml.gz";
		DgNetworkUtils.writeNetwork(smallNetwork, smallNetworkClean);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, crs, shapeFileDirectory + "network_small_clean");
		
		//run a network simplifier to merge links with same attributes
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		if (simplifyNetwork){
			nodeTypesToMerge.add(NetworkCalcTopoType.PASS1WAY); //PASS1WAY: 1 in- and 1 outgoing link
			nodeTypesToMerge.add(NetworkCalcTopoType.PASS2WAY); //PASS2WAY: 2 in- and 2 outgoing links
		}
		NetworkLanesSignalsSimplifier nsimply = new NetworkLanesSignalsSimplifier();
		nsimply.setNodesToMerge(nodeTypesToMerge);
		nsimply.setSimplifySignalizedNodes(false);
		nsimply.setMaximalLinkLength(maximalLinkLength);
		nsimply.simplifyNetworkLanesAndSignals(smallNetwork, this.fullScenario.getLanes(), (SignalsData) this.fullScenario.getScenarioElement(SignalsData.ELEMENT_NAME));
		this.originalToSimplifiedLinkIdMatching = nsimply.getOriginalToSimplifiedLinkIdMatching();
		
		this.shrinkedNetwork =  smallNetwork;
		this.shrinkedLanes = this.fullScenario.getLanes();
		this.shrinkedSignals = (SignalsData) this.fullScenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		LanesConsistencyChecker lanesConsistency = new LanesConsistencyChecker(smallNetwork, shrinkedLanes);
		lanesConsistency.checkConsistency();
		
		SignalSystemsDataConsistencyChecker signalsConsistency = new SignalSystemsDataConsistencyChecker(this.shrinkedNetwork, this.shrinkedLanes, this.shrinkedSignals);
		signalsConsistency.checkConsistency();

		//write shrunk data to disk
		String simplifiedNetworkFile = outputDirectory + simplifiedNetworkFilename;
		DgNetworkUtils.writeNetwork(smallNetwork, simplifiedNetworkFile);
		DgNetworkUtils.writeNetwork2Shape(smallNetwork, crs, shapeFileDirectory + "network_small_simplified");

		LaneDefinitionsWriter20 lanesWriter = new LaneDefinitionsWriter20(this.fullScenario.getLanes());
		lanesWriter.write(outputDirectory + simplifiedLanesFilename);
		
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter(outputDirectory);
		signalsWriter.writeSignalsData((SignalsData) this.fullScenario.getScenarioElement(SignalsData.ELEMENT_NAME));
		
	}
		
	public DgSignalsBoundingBox getCuttingBoundingBox() {
		return this.cuttingBoundingBox;
	}
	
	public Map<Id<Link>, Id<Link>> getOriginalToSimplifiedLinkIdMatching(){
		return this.originalToSimplifiedLinkIdMatching;
	}
	

	private Set<Id<Node>> getSignalizedNodeIds(SignalSystemsData signals, Network network){
		Map<Id<SignalSystem>, Set<Id<Node>>> signalizedNodesPerSystem = DgSignalsUtils.calculateSignalizedNodesPerSystem(signals, network);
		Set<Id<Node>> signalizedNodes = new HashSet<>();
		for (Set<Id<Node>> signalizedNodesOfSystem : signalizedNodesPerSystem.values()){
			signalizedNodes.addAll(signalizedNodesOfSystem);
		}
		return signalizedNodes;
	}

	
	public Network getShrinkedNetwork() {
		return shrinkedNetwork;
	}

	
	public Lanes getShrinkedLanes() {
		return shrinkedLanes;
	}

	
	public SignalsData getShrinkedSignals() {
		return shrinkedSignals;
	}
	
	
}
