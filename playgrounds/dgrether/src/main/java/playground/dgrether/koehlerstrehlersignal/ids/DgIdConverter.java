/* *********************************************************************** *
 * project: org.matsim.*
 * DgIdConverter
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
package playground.dgrether.koehlerstrehlersignal.ids;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.signals.model.SignalSystem;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.data.TtPath;


/**
 * The KS2010 model uses different and more ids than MATSim. 
 * This class implements the symbolic conversion functions (String -> String). 
 * Intern, a IdPool is used that holds a mapping from each String id to an integer. 
 * The IdPool is required to prevent integer overflows on the cplex side. 
 * 
 * @author dgrether
 *
 */
public class DgIdConverter {

	private static final Logger log = Logger.getLogger(DgIdConverter.class);
	
	private DgIdPool idPool;
	
	public DgIdConverter(DgIdPool idpool){
		this.idPool = idpool;
	}
	
	/**
	 * creates an id for the crossing node of the extended crossing corresponding to
	 * the FromNode of the link. in the extended ks-model network the street 
	 * corresponding to the matsim link will start at this crossing node.
	 * 
	 * @param linkId the id of the link in the matsim network
	 * @return the id of the corresponding FromNode in the ks-model
	 */
	public  Id<DgCrossingNode> convertLinkId2FromCrossingNodeId(Id<Link> linkId){
		String idString = linkId.toString() + "11";
		return idPool.createId(idString, DgCrossingNode.class);
	}
	
	/**
	 * creates an id for the crossing node of the extended crossing corresponding to
	 * the ToNode of the link. in the extended ks-model network the street 
	 * corresponding to the matsim link will end at this crossing node.
	 * 
	 * @param linkId the id of the link in the matsim network
	 * @return the id of the corresponding ToNode in the ks-model
	 */
	public Id<DgCrossingNode> convertLinkId2ToCrossingNodeId(Id<Link> linkId){
		String idString = linkId.toString() + "99";
		return idPool.createId(idString, DgCrossingNode.class);
	}
	
	/**
	 * converts back. see convertLinkId2ToCrossingNodeId(...)
	 * 
	 * @param toCrossingNodeId the id of a crossing node in the ks-model network
	 * @return the id of the matsim link corresponding to the street ending in this crossing node
	 */
	public Id<Link> convertToCrossingNodeId2LinkId(Id<DgCrossingNode> toCrossingNodeId){
		Integer ksIntToCrossingNodeId = Integer.parseInt(toCrossingNodeId.toString());
		String matsimStringLinkId = this.idPool.getStringId(ksIntToCrossingNodeId);
		if (matsimStringLinkId.endsWith("99")){
			Id<Link> id = Id.create(matsimStringLinkId.substring(0, matsimStringLinkId.length() - 2), Link.class);
			return id;
		}
		throw new IllegalStateException("Can not convert " + matsimStringLinkId + " to link id");
	}
	
	/**
	 * creates a light id for a link to link relationship
	 * 
	 * @param fromLinkId
	 * @param fromLaneId
	 * @param toLinkId
	 * @return the light id
	 */
	public Id<DgGreen> convertFromLinkIdToLinkId2LightId(Id<Link> fromLinkId, Id<Lane> fromLaneId, Id<Link> toLinkId){
		String id =  null;
		if (fromLaneId == null){
			id = fromLinkId.toString()  + "55" + toLinkId.toString();
		}
		else {
			id = fromLinkId.toString() + "66" + fromLaneId.toString() + "55" + toLinkId.toString();
		}
		String idString = id.toString();
		return idPool.createId(idString, DgGreen.class);
	}
	
	/**
	 * creates an id for the crossing representing the matsim node in the ks-model
	 * 
	 * @param nodeId the matsim node id
	 * @return the corresponding crossing id in the ks-model
	 */
	public  Id<DgCrossing> convertNodeId2CrossingId(Id<Node> nodeId){
		String idString = nodeId.toString() + "77";
		return idPool.createId(idString, DgCrossing.class);
	}
	
	/**
	 * converts back. see convertNodeId2CrossingId(...)
	 * 
	 * @param crossingId the crossing id in the ks-model
	 * @return the corresponding node id in the matsim network
	 */
	public Id<Node> convertCrossingId2NodeId(Id<DgCrossing> crossingId){
		Integer ksIntCrossingId = Integer.parseInt(crossingId.toString());
		String matsimStringNodeId = this.idPool.getStringId(ksIntCrossingId);
		if (matsimStringNodeId.endsWith("77")){
			Id<Node> id = Id.create(matsimStringNodeId.substring(0, matsimStringNodeId.length() - 2), Node.class);
			return id;
		}
		throw new IllegalStateException("Can not convert " + matsimStringNodeId + " to node id");
	}
	
	/**
	 * converts a matsim node ID of a node outside the signals bounding box 
	 * to the single crossing node ID existing for the not expanded crossing in the ks-model.
	 * (the signals bounding box determines the region of spatial expansion: all nodes within this area will be expanded.)
	 * 
	 * @param nodeId the id of the matsim node
	 * @return the crossing node id in the ks-model representing the single crossing node 
	 * of the not expanded crossing corresponding to the matsim node
	 */
	public Id<DgCrossingNode> convertNodeId2NotExpandedCrossingNodeId(Id<Node> nodeId){
		String idString = nodeId.toString();
		return idPool.createId(idString, DgCrossingNode.class);
	}
	
	/**
	 * converts back. see convertNodeId2NotExpandedCrossingNodeId(...)
	 * 
	 * @param crossingId the crossing node id in the ks-model
	 * @return the id of the matsim node corresponding to the not expanded crossing 
	 * in the ks-model containing this crossing node
	 */
	public Id<Node> convertNotExpandedCrossingNodeId2NodeId(Id<DgCrossingNode> crossingNodeId){
		Integer ksIntCrossingNodeId = Integer.parseInt(crossingNodeId.toString());
		String matsimStringCrossingNodeId = this.idPool.getStringId(ksIntCrossingNodeId);
		return Id.create(matsimStringCrossingNodeId, Node.class);
	}
	
	/**
	 * creates a street id for the ks-model corresponding to the link id of the matsim network
	 * 
	 * @param linkId the link id in the matsim network
	 * @return the street id for the ks-model
	 */
	public Id<DgStreet> convertLinkId2StreetId(Id<Link> linkId){
		String idString = linkId.toString() + "88";
		return idPool.createId(idString, DgStreet.class);
	}

	/**
	 * converts back. see convertLinkId2StreetId(...)
	 * 
	 * @param streetId the street id in the ks-model
	 * @return the corresponding link id in the matsim network
	 */
	public Id<Link> convertStreetId2LinkId(Id<DgStreet> streetId){
		Integer ksIntStreetId = Integer.parseInt(streetId.toString());
		String matsimStringStreetId = this.idPool.getStringId(ksIntStreetId);
		if (matsimStringStreetId.endsWith("88")){
			Id<Link>  id = Id.create(matsimStringStreetId.substring(0, 
					matsimStringStreetId.length() - 2), Link.class);
			return id;
		}
		throw new IllegalStateException("Can not convert " + matsimStringStreetId + " to link id");
	}

	/**
	 * create a commodity id for the FromLink - ToLink pair
	 * 
	 * @param fromLinkId
	 * @param toLinkId
	 * @return commodity id
	 */
	public Id<DgCommodity> convertLinkToLinkPair2CommodityId(Id<Link> fromLinkId, Id<Link> toLinkId){
		String idString = fromLinkId + "33" + toLinkId;
		return idPool.createId(idString, DgCommodity.class);
	}

	/**
	 * Creates an id for a path in the ks model
	 * 
	 * @param ksPath the path, i.e. a list of street ids in the ks model 
	 * @param ksSourceNodeId the source node id in the ks model
	 * @param ksDrainNodeId the drain node id in the ks model
	 * @return the path id in the ks model
	 */
	public Id<TtPath> convertPathInfo2PathId(List<Id<DgStreet>> ksPath, 
			Id<DgCrossingNode> ksSourceNodeId, Id<DgCrossingNode> ksDrainNodeId) {
		// add source node id
		String idString = ksSourceNodeId.toString() + "22";
		// add street ids
		for (Id<DgStreet> streetId : ksPath){
			idString += streetId.toString() + "44";
		}
		// delete last "44"
		idString.substring(0,idString.length()-3);
		// add drain node id
		idString += "22" + ksDrainNodeId.toString();
		return idPool.createId(idString, TtPath.class);
	}

	/**
	 * Creates a program id for the ks model corresponding to the signal system id
	 * in the matsim network
	 * 
	 * @param signalSystemId the signal system id in the matsim network
	 * @return the program id in the ks model
	 */
	public Id<DgProgram> convertSignalSystemId2ProgramId(Id<SignalSystem> signalSystemId) {
		String idString = signalSystemId.toString() + "00";
		return idPool.createId(idString, DgProgram.class);
	}

	/**
	 * Converts back. See convertSignalSystemId2ProgramId(...)
	 * 
	 * @param programId the program id in the ks model
	 * @return the corresponding signal system id in the matsim network
	 */
	public Id<SignalSystem> convertProgramId2SignalSystemId(
			Id<DgProgram> programId) {
		Integer ksIntProgramId = Integer.parseInt(programId.toString());
		String matsimStringProgramId = this.idPool.getStringId(ksIntProgramId);
		if (matsimStringProgramId.endsWith("00")){
			Id<SignalSystem> id = Id.create(matsimStringProgramId.substring(0, 
					matsimStringProgramId.length() - 2), SignalSystem.class);
			return id;
		}
		throw new IllegalStateException("Can not convert " + matsimStringProgramId 
				+ " to signal system id");
	}
	
}
