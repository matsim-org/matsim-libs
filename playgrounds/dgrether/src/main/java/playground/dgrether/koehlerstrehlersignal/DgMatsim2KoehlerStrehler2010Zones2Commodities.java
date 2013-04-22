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
package playground.dgrether.koehlerstrehlersignal;

import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZoneFromLink;


/**
 * @author dgrether
 *
 */
public class DgMatsim2KoehlerStrehler2010Zones2Commodities implements
		DgMatsim2KoehlerStrehler2010DemandConverter {

	private Map<DgZone, Link> zones2LinkMap;
	private DgIdConverter idConverter;

	public DgMatsim2KoehlerStrehler2010Zones2Commodities(Map<DgZone, Link> zones2LinkMap, DgIdConverter idConverter) {
		this.zones2LinkMap = zones2LinkMap;
		this.idConverter = idConverter;
	}

	private void addCommodity(DgCommodities coms, Id id, Id fromNodeId, Id toNodeId, Double flow, DgKSNetwork net){
		this.validateFromAndToNode(fromNodeId, toNodeId, net);
		DgCommodity com = new DgCommodity(id);
		coms.addCommodity(com);
		com.setSourceNode(fromNodeId, flow);
		com.setDrainNode(toNodeId);
	}
	
	private void validateFromAndToNode(Id fromNode, Id toNode, DgKSNetwork net){
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

	
	
	@Override
	public DgCommodities convert(Scenario sc, DgKSNetwork network) {
		DgCommodities coms = new DgCommodities();
		for (DgZone fromZone : this.zones2LinkMap.keySet()){
			Link fromZoneLink = this.zones2LinkMap.get(fromZone);
			Id fromNodeId = this.idConverter.convertLinkId2ToCrossingNodeId(fromZoneLink.getId());
			
			//zone 2 zone
			for (Entry<DgZone, Double> entry : fromZone.getToZoneRelations().entrySet()){
				Id id = this.idConverter.createFromZone2ToZoneId(fromZone.getId(), entry.getKey().getId());
				Link toZoneLink = this.zones2LinkMap.get(entry.getKey());
				Id toNodeId = this.idConverter.convertLinkId2FromCrossingNodeId(toZoneLink.getId());
				this.addCommodity(coms, id, fromNodeId, toNodeId, entry.getValue(), network);
			}
			//zone 2 link
			for (Entry<Link, Double> entry : fromZone.getToLinkRelations().entrySet()){
				Id toNodeId = this.idConverter.convertLinkId2FromCrossingNodeId(entry.getKey().getId());
				Id id = this.idConverter.createFrom2ToId(fromZone.getId(), entry.getKey().getId().toString());
				this.addCommodity(coms, id, fromNodeId, toNodeId, entry.getValue(), network);
			}
			//link 2 x
			for (DgZoneFromLink fromLink : fromZone.getFromLinks().values()){
				Id fromNodeId2 = this.idConverter.convertLinkId2ToCrossingNodeId(fromLink.getLink().getId());
				//link 2 zone
				for (Entry<DgZone, Double> entry : fromLink.getToZoneRelations().entrySet()){
					Id id = this.idConverter.createFrom2ToId(fromLink.getLink().getId().toString(), entry.getKey().getId());
					Link toZoneLink = this.zones2LinkMap.get(entry.getKey());
					Id toNodeId = this.idConverter.convertLinkId2FromCrossingNodeId(toZoneLink.getId());
					this.addCommodity(coms, id, fromNodeId2, toNodeId, entry.getValue(), network);
				}
				//link 2 link
				for (Entry<Link, Double> entry : fromLink.getToLinkRelations().entrySet()){
					Id id = this.idConverter.createFromLink2ToLinkId(fromLink.getLink().getId().toString(), entry.getKey().getId().toString());
					Id toNodeId = this.idConverter.convertLinkId2FromCrossingNodeId(entry.getKey().getId());
					this.addCommodity(coms, id, fromNodeId2, toNodeId, entry.getValue(), network);
				}
			}			
		}
		return coms;
	}

}
