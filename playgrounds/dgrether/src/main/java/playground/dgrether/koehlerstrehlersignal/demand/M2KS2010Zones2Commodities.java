/* *********************************************************************** *
 * project: org.matsim.*
 * DgMatsim2KoehlerStrehler2010Zones2Commodities
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.demand;

import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.TtCrossingType;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZoneFromLink;
import playground.dgrether.utils.zones.DgZones;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class M2KS2010Zones2Commodities  {

	private DgZones zones;
	private DgIdConverter idConverter;

	public M2KS2010Zones2Commodities(DgZones zones, DgIdConverter idConverter) {
		this.zones = zones;
		this.idConverter = idConverter;
	}

	private void addCommodity(DgCommodities coms, Id<DgCommodity> id, Id<DgCrossingNode> fromNodeId, Id<DgCrossingNode> toNodeId, Id<Link> fromLinkId, Id<Link> toLinkId, Double flow, DgKSNetwork net){
		this.validateFromAndToNode(fromNodeId, toNodeId, net);
		DgCommodity com = new DgCommodity(id);
		coms.addCommodity(com);
		com.setSourceNode(fromNodeId, fromLinkId, flow);
		com.setDrainNode(toNodeId, toLinkId);
	}
	
	private void validateFromAndToNode(Id<DgCrossingNode> fromNode, Id<DgCrossingNode> toNode, DgKSNetwork net){
		boolean foundFrom = false;
		boolean foundTo = false;
		for (DgCrossing crossing : net.getCrossings().values()){
			if (crossing.getNodes().containsKey(fromNode)) {
				foundFrom = true;
			}
			if (crossing.getNodes().containsKey(toNode)){
				foundTo = true;
			}
		}
		if (! foundFrom){
			throw new IllegalStateException("From Node Id " + fromNode + " not found in Network. ");
		}
		if (! foundTo){
			throw new IllegalStateException("To  Node Id " + toNode + " not found in Network. ");
		}
	}
	
	// converts commodities in link to link representation only 
	// (old code for zone to zone, zone to link and link to zone representations can be found in the svn revision 25203 from 19.7.2013)
	public DgCommodities convert(DgKSNetwork network) {
		DgCommodities coms = new DgCommodities();
		for (DgZone fromZone : this.zones.values()){
			for (DgZoneFromLink fromLink : fromZone.getFromLinks().values()){
				// use the down-stream node of the fromLink as fromNode for the commodity
				// Note: until nov'14 we used the up-stream node here. problem: cplex considers the travel time of the first link while matsim doesn't do so.
				Id<Node> fromNodeId = fromLink.getLink().getToNode().getId(); // the matsim from node id
				Id<DgCrossing> fromCrossingId = this.idConverter.convertNodeId2CrossingId(fromNodeId); // the ks-model crossing id
				Id<DgCrossingNode> fromCrossingNodeId; // the ks-model crossing node id
				// check whether the from crossing is expanded or not to determine the from crossing node id
				if (network.getCrossings().get(fromCrossingId).getType().equals(TtCrossingType.NOTEXPAND)){
					fromCrossingNodeId = this.idConverter.convertNodeId2NotExpandedCrossingNodeId(fromNodeId);
				}
				else{
					fromCrossingNodeId = this.idConverter.convertLinkId2ToCrossingNodeId(fromLink.getLink().getId());
				}
				
				for (Entry<Link, Double> toLinkEntry : fromLink.getDestinationLinkTrips().entrySet()){
					Id<DgCommodity> id = this.idConverter.convertLinkToLinkPair2CommodityId(fromLink.getLink().getId(), toLinkEntry.getKey().getId());
					// uses the down-stream node of the toLink as toNode for the commodity
					Id<Node> toNodeId = toLinkEntry.getKey().getToNode().getId(); // the matsim to node id
					Id<DgCrossing> toCrossingId = this.idConverter.convertNodeId2CrossingId(toNodeId); // the ks-model crossing id
					Id<DgCrossingNode> toCrossingNodeId; // the ks-model crossing node id
					// check whether the to crossing is expanded or not to determine the to crossing node id
					if (network.getCrossings().get(toCrossingId).getType().equals(TtCrossingType.NOTEXPAND)){
						toCrossingNodeId = this.idConverter.convertNodeId2NotExpandedCrossingNodeId(toNodeId);
					}
					else{
						toCrossingNodeId = this.idConverter.convertLinkId2ToCrossingNodeId(toLinkEntry.getKey().getId());
					}
					
					if (! fromCrossingNodeId.equals(toCrossingNodeId))
						this.addCommodity(coms, id, fromCrossingNodeId, toCrossingNodeId, fromLink.getLink().getId(), toLinkEntry.getKey().getId(), 
							toLinkEntry.getValue(), network);
				}
			}			
		}
		return coms;
	}

}
