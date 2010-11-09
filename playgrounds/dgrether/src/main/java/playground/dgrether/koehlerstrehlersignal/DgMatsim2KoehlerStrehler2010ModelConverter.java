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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupData;
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
import playground.dgrether.signalsystems.DgSignalsUtils;


/**
 * @author dgrether
 *
 */
public class DgMatsim2KoehlerStrehler2010ModelConverter {
	
	private static final Logger log = Logger.getLogger(DgMatsim2KoehlerStrehler2010ModelConverter.class);

	public static final String CROSSING_TO_NODE_SUFFIX = "_to";
	public static final String CROSSING_FROM_NODE_SUFFIX = "_from";
	
	private int cycle = 60;
	private Id programId = new IdImpl("1");
	
	private Id convertLinkId2FromCrossingNodeId(Id linkId){
		return new IdImpl(linkId.toString() + CROSSING_FROM_NODE_SUFFIX);
	}
	
	private Id convertLinkId2ToCrossingNodeId(Id linkId){
		return new IdImpl(linkId.toString() + CROSSING_TO_NODE_SUFFIX);
	}
	
	
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
	private DgNetwork createNetwork(Network net, LaneDefinitions lanes, SignalsData signalsData) {
		
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
		Set<Id> signalizedLinks = this.getSigalizedLinkIds(signalsData.getSignalSystemsData());
		//loop over links and create layout of crossing
		for (Link link : net.getLinks().values()){
			//prepare some objects/data
			DgCrossing crossing = dgnet.getCrossings().get(link.getToNode().getId());
			Link backLink = this.getBackLink(link);
			DgCrossingNode inLinkToNode = crossing.getNodes().get(this.convertLinkId2ToCrossingNodeId(link.getId()));
			LanesToLinkAssignment l2l = lanes.getLanesToLinkAssignments().get(link.getId());
			//create crossing layout
			if (signalizedLinks.contains(link.getId())){
				SignalSystemData system = this.getSignalSystem4SignalizedLinkId(signalsData.getSignalSystemsData(), link.getId());
				this.createCrossing4SignalizedLink(crossing, link, inLinkToNode, backLink, l2l, system, signalsData);
			}
			else {
				this.createCrossing4NotSignalizedLink(crossing, link, inLinkToNode, backLink, l2l);
			}
		}
		return dgnet;
	}

	public Tuple<SignalPlanData, SignalGroupSettingsData> getPlanAndSignalGroupSettings4Signal(Id signalSystemId, Id signalId, SignalsData signalsData){
		SignalSystemControllerData controllData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(signalSystemId);
		SignalPlanData signalPlan = controllData.getSignalPlanData().values().iterator().next();
		SignalGroupData signalGroup = DgSignalsUtils.getSignalGroup4SignalId(signalSystemId, signalId, signalsData.getSignalGroupsData());
		return new Tuple<SignalPlanData, SignalGroupSettingsData>(signalPlan, signalPlan.getSignalGroupSettingsDataByGroupId().get(signalGroup.getId()));
	}

	

	private Set<Id> getSigalizedLinkIds(SignalSystemsData signals){
		Map<Id, Set<Id>> signalizedLinksPerSystem = DgSignalsUtils.calculateSignalizedLinksPerSystem(signals);
		Set<Id> signalizedLinks = new HashSet<Id>();
		for (Set<Id> signalizedLinksOfSystem : signalizedLinksPerSystem.values()){
			signalizedLinks.addAll(signalizedLinksOfSystem);
		}
		return signalizedLinks;
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
	
	
	private void createCrossing4SignalizedLink(DgCrossing crossing, Link link, DgCrossingNode inLinkToNode, Link backLink, LanesToLinkAssignment l2l, SignalSystemData system, SignalsData signalsData) {
		List<SignalData> signals4Link = this.getSignals4LinkId(system, link.getId());
		DgProgram program = crossing.getPrograms().get(this.programId);
		if (signals4Link.size() == 1){
			SignalData signal = signals4Link.get(0);
			Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
			SignalPlanData signalPlan = planGroupSettings.getFirst();
			SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
			if (l2l == null) {
				List<Link> toLinks = this.getTurningMoves4LinkWoLanes(link);
				for (Link outLink : toLinks){
					Id lightId = this.createLights(link.getId(), outLink.getId(), backLink.getId(), inLinkToNode, crossing);
					if (lightId != null){
						this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
					}
				}
			}
			else {
				for (Lane lane : l2l.getLanes().values()){
					if (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()){ // check for outlane
						for (Id outLinkId : lane.getToLinkIds()){
							Id lightId = this.createLights(link.getId(), outLinkId, backLink.getId(), inLinkToNode, crossing);
							if (lightId != null){
								this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
							}
						}
					}
				}
			}
		}
		else { //more than one signal on this link
			//TODO can i save half of the code?
			if (l2l == null){
				for (SignalData signal : signals4Link){
					if (signal.getTurningMoveRestrictions() == null || signal.getTurningMoveRestrictions().isEmpty()){
						throw new IllegalStateException("more than one signal on one link but no lanes and no turning move restrictions is not allowed");
					}
					else { // we have turning move restrictions
						for (Id outLinkId : signal.getTurningMoveRestrictions()){
							Id lightId = this.createLights(link.getId(), outLinkId, backLink.getId(), inLinkToNode, crossing);
							if (lightId != null){
								Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
								SignalPlanData signalPlan = planGroupSettings.getFirst();
								SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
								this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
							}
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
							if (lightId != null){
								Tuple<SignalPlanData, SignalGroupSettingsData> planGroupSettings = this.getPlanAndSignalGroupSettings4Signal(system.getId(), signal.getId(), signalsData);
								SignalPlanData signalPlan = planGroupSettings.getFirst();
								SignalGroupSettingsData groupSettings = planGroupSettings.getSecond();
								this.createAndAddGreen4Settings(lightId, program, groupSettings, signalPlan);
							}
						}
					}
				}
			}
		}
	}
	

	private void createCrossing4NotSignalizedLink(DgCrossing crossing, Link link,
			DgCrossingNode inLinkToNode, Link backLink, LanesToLinkAssignment l2l) {
		DgProgram program = crossing.getPrograms().get(this.programId);
		if (l2l == null){
			List<Link> toLinks = this.getTurningMoves4LinkWoLanes(link);
			for (Link outLink : toLinks){
				Id lightId = this.createLights(link.getId(), outLink.getId(), backLink.getId(), inLinkToNode, crossing);
				if (lightId != null){
					this.createAndAddAllTimeGreen(lightId, program);
				}
			}
		}
		else {
			for (Lane lane : l2l.getLanes().values()){
				if (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()){ // check for outlanes
					for (Id outLinkId : lane.getToLinkIds()){
						Id lightId = this.createLights(link.getId(), outLinkId, backLink.getId(), inLinkToNode, crossing);
						if (lightId != null){
							this.createAndAddAllTimeGreen(lightId, program);
						}
					}
				}
			}
		}
	}

	private void createAndAddGreen4Settings(Id lightId, DgProgram program,
			SignalGroupSettingsData groupSettings, SignalPlanData signalPlan) {
		DgGreen green = new DgGreen(lightId);
		green.setOffset(signalPlan.getOffset());
		green.setLength(this.calculateGreenTimeSeconds(groupSettings, signalPlan.getCycleTime()));
		program.addGreen(green);
	}
	
	
	public int calculateGreenTimeSeconds(SignalGroupSettingsData settings, Integer cycle){
		if (settings.getOnset() <= settings.getDropping()) {
			return settings.getDropping() - settings.getOnset();
		}
		else {
			return  settings.getDropping() + (cycle - settings.getOnset()); 
		}
	}

	
	
	private void createAndAddAllTimeGreen(Id lightId, DgProgram program){
		DgGreen green = new DgGreen(lightId);
		green.setLength(this.cycle);
		green.setOffset(0);
		program.addGreen(green);
	}
	
	
	private SignalSystemData getSignalSystem4SignalizedLinkId(SignalSystemsData signalSystems, Id linkId){
		for (SignalSystemData system : signalSystems.getSignalSystemData().values()){
			for (SignalData signal : system.getSignalData().values()){
				if (signal.getLinkId().equals(linkId)){
					return system;
				}
			}
		}
		return null;
	}
	
	private List<SignalData> getSignals4LinkId(SignalSystemData system, Id linkId){
		List<SignalData> signals4Link = new ArrayList<SignalData>();
		for (SignalData signal : system.getSignalData().values()){
			if (signal.getLinkId().equals(linkId)){
				signals4Link.add(signal);
			}
		}
		return signals4Link;
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
