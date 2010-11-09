/* *********************************************************************** *
 * project: org.matsim.*
 * DgMatsim2KoehlerStrehler2010ModelConverter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.xml.sax.SAXException;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
import playground.dgrether.koehlerstrehlersignal.data.DgNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;


/**
 * @author dgrether
 *
 */
public class DgMatsim2KoehlerStrehler2010ModelConverter {
	
	public static final String CROSSING_TO_NODE_SUFFIX = "_to";
	public static final String CROSSING_FROM_NODE_SUFFIX = "_from";
	
	private int cycle = 60;
	private Id programId = new IdImpl("1");
	
	private static final Logger log = Logger
			.getLogger(DgMatsim2KoehlerStrehler2010ModelConverter.class);
	
	
	public void convertAndWrite(ScenarioImpl sc, String outFile) throws SAXException, IOException, TransformerConfigurationException{
		DgNetwork network = this.createNetwork(sc.getNetwork(), sc.getLaneDefinitions(), sc.getScenarioElement(SignalsData.class));
		DgKoehlerStrehler2010ModelWriter writer = new DgKoehlerStrehler2010ModelWriter();
		writer.write(sc, network, outFile);
	}
	
	private void convertNodes2Crossings(DgNetwork dgnet, Network net){
		for (Node node : net.getNodes().values()){
			DgCrossing crossing = new DgCrossing(node.getId());
			dgnet.addCrossing(crossing);
			DgProgram program = new DgProgram(this.programId);
			crossing.addProgram(program);
			program.setCycle(this.cycle);
		}
	}
	
	private Id convertLinkId2FromCrossingNodeId(Id linkId){
		return new IdImpl(linkId.toString() + CROSSING_FROM_NODE_SUFFIX);
	}

	private Id convertLinkId2ToCrossingNodeId(Id linkId){
		return new IdImpl(linkId.toString() + CROSSING_TO_NODE_SUFFIX);
	}

	
	private void convertLinks2Streets(DgNetwork dgnet, Network net){
		for (Link link : net.getLinks().values()){
			DgCrossing fromNodeCrossing = dgnet.getCrossings().get(link.getFromNode().getId());
			DgCrossingNode fromNode = new DgCrossingNode(this.convertLinkId2FromCrossingNodeId(link.getId()));
			fromNodeCrossing.addNode(fromNode);
			DgCrossing toNodeCrossing = dgnet.getCrossings().get(link.getToNode().getId());
			DgCrossingNode toNode = new DgCrossingNode(this.convertLinkId2ToCrossingNodeId(link.getId()));
			toNodeCrossing.addNode(toNode);
			DgStreet street = new DgStreet(link.getId(), fromNode, toNode);
			dgnet.addStreet(street);
		}
	}

	
	/*
	 * codierung:
	 *   fromLink -> toLink zwei nodes + 1 light
	 * a) calculate signalized nodes:
	 *   a1) nodes with only one node no lanes
	 *   a2) nodes with only one node with lanes
	 *   a3) nodes with more than one node no lanes
	 *   a4) nodes with more than one node with lanes
	 * b) calculate turning moves:
	 *   b1) calculate turning moves from a1 + a2: fromLink -> toLink no back links
	 *   b2) do the same with a3 and a4 but other algorithms?
	 * c) for all b1: 
	 *     create two new nodes per turning move, store the id of the fromLinkNode as new end node of the from link, same with toLink
	 *     create light (connection) per turning move, connecting the two new nodes
	 */
	private DgNetwork createNetwork(Network net, LaneDefinitions laneDefinitions, SignalsData signalsData) {
		
		DgNetwork dgnet = new DgNetwork();
		
		/* create a crossing for each node, same id
		 */
		this.convertNodes2Crossings(dgnet, net);
		/*
		 * convert all links to streets (same id) and create the from and to 
		 * nodes (ids generated from link id) for the already created corresponding 
		 * crossing 
		 */
		this.convertLinks2Streets(dgnet, net);


		//collect all ids of links that are signalized
		Set<Id> signalizedLinks = null;
		//loop rather over links so nothing is forgotten
		for (Link link : net.getLinks().values()){
			this.createCrossing4SignalizedLink(dgnet, link, laneDefinitions, signalsData.getSignalSystemsData(), signalizedLinks);
		}
		return dgnet;
	}


	
	/**
	 * 
	 */
	private Id createLights(Id fromLinkId, Id outLinkId, Id backLinkId, DgCrossingNode inLinkToNode, DgCrossing crossing){
		if (backLinkId != null && backLinkId.equals(outLinkId)){
			return null; //do nothing if it is the backlink
		}
		Id lightId = new IdImpl(fromLinkId.toString() + "_" + outLinkId.toString());
		DgCrossingNode outLinkFromNode = crossing.getNodes().get(this.convertLinkId2FromCrossingNodeId(outLinkId));
		DgStreet light = new DgStreet(lightId, inLinkToNode, outLinkFromNode);
		crossing.addLight(light);
		return lightId;
	}
	
	
	private void createCrossing4SignalizedLink(DgNetwork dgnet, Link link, LaneDefinitions lanes, SignalSystemsData signalSystems, Set<Id> signalizedLinks) {
		SignalSystemData system;
		List<SignalData> signals4Link = null;
		DgCrossing crossing = dgnet.getCrossings().get(link.getToNode().getId());
		DgProgram program = crossing.getPrograms().get(this.programId);
		Link backLink = this.getBackLink(link);
		DgCrossingNode inLinkToNode = crossing.getNodes().get(this.convertLinkId2ToCrossingNodeId(link.getId()));
		LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(link.getId());
		if (signals4Link.size() == 1){
			if (l2l == null) {
				List<Link> toLinks = this.getTurningMoves4LinkWoLanes(link);
				for (Link outLink : toLinks){
					Id lightId = this.createLights(link.getId(), outLink.getId(), backLink.getId(), inLinkToNode, crossing);
					//create green for the light TODO
					DgGreen green = new DgGreen(lightId);
					green.setLength(this.cycle);
					green.setOffset(0);
					program.addGreen(green);
				}
			}
			else {
				for (Lane lane : l2l.getLanes().values()){
					if (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()){ // check for outlane
						for (Id outLinkId : lane.getToLinkIds()){
							Id lightId = this.createLights(link.getId(), outLinkId, backLink.getId(), inLinkToNode, crossing);
							//create green for the light TODO
							DgGreen green = new DgGreen(lightId);
							green.setLength(this.cycle);
							green.setOffset(0);
							program.addGreen(green);
						}
					}
				}
			}
		}
		else { //more than one signal on this link
			if (l2l == null){
				for (SignalData signal : signals4Link){
					if (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty()){
						throw new IllegalStateException("more than one signal on one link but no lanes and no turning move restrictions is not allowed");
					}
					else { // we have turning move restrictions
						for (Id outLinkId : signal.getTurningMoveRestrictions()){
							Id lightId = this.createLights(link.getId(), outLinkId, backLink.getId(), inLinkToNode, crossing);
							//create green for the light TODO
							DgGreen green = new DgGreen(lightId);
							green.setLength(this.cycle);
							green.setOffset(0);
							program.addGreen(green);
						}
					}
				}
			}
			else { //lane with links
				for (SignalData signal : signals4Link){
					for (Id laneId : signal.getLaneIds()){
						Lane lane = l2l.getLanes().get(laneId);
						for (Id outLinkId : lane.getToLinkIds()){
							Id lightId = this.createLights(link.getId(), outLinkId, backLink.getId(), inLinkToNode, crossing);
							//create green for the light TODO
							DgGreen green = new DgGreen(lightId);
							green.setLength(this.cycle);
							green.setOffset(0);
							program.addGreen(green);
						}
					}
				}
			}
		}
		
	}

	
	private Link getBackLink(Link link){
		for (Link outLink : link.getToNode().getOutLinks().values()){
			if (link.getFromNode().equals(outLink.getToNode())){
				return outLink;
			}
		}
		return null;
	}
	
	
	private List<Link> getTurningMoves4LinkWoLanes(Link link){
		List<Link> outLinks = new ArrayList<Link>();
		for (Link outLink : link.getToNode().getOutLinks().values()){
			if (!link.getFromNode().equals(outLink.getToNode())){
				outLinks.add(outLink);
			}
		}
		return outLinks;
	}
	
	
	/**
	 * @param args
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws TransformerConfigurationException 
	 */
	public static void main(String[] args) throws SAXException, TransformerConfigurationException, IOException {
		ScenarioImpl sc = new DgKoehlerStrehler2010ScenarioGenerator().loadScenario();
		new DgMatsim2KoehlerStrehler2010ModelConverter().convertAndWrite(sc, DgPaths.STUDIESDG + "koehlerStrehler2010/cplex_metamodel_input.xml");
	}
}
