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
import org.matsim.core.basic.v01.IdImpl;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZoneFromLink;


/**
 * @author dgrether
 *
 */
public class DgMatsim2KoehlerStrehler2010Zones2Commodities implements
		DgMatsim2KoehlerStrehler2010DemandConverter {

	private Map<DgZone, Link> zones2LinkMap;

	public DgMatsim2KoehlerStrehler2010Zones2Commodities(Map<DgZone, Link> zones2LinkMap) {
		this.zones2LinkMap = zones2LinkMap;
	}

	private void addCommodity(DgCommodities coms, Id id, Id fromNodeId, Id toNodeId, Double flow ){
		DgCommodity com = new DgCommodity(id);
		coms.addCommodity(com);
		com.addSourceNode(fromNodeId, flow);
		com.addDrainNode(toNodeId);
	}
	
	public static Id createFromZone2ToZoneId(String from, String to){
		return new IdImpl(from + "22" + to);
	}

	public static Id createFromLink2ToLinkId(String from, String to){
		return new IdImpl(from + "33" + to);
	}
	
	public static Id createFrom2ToId(String from, String to){
		return new IdImpl(from + "44" + to);
	}

	
	
	@Override
	public DgCommodities convert(Scenario sc, DgKSNetwork dgNetwork) {
		DgCommodities coms = new DgCommodities();
		for (DgZone fromZone : this.zones2LinkMap.keySet()){
			Link fromZoneLink = this.zones2LinkMap.get(fromZone);
			Id fromNodeId = DgMatsim2KoehlerStrehler2010NetworkConverter.convertLinkId2ToCrossingNodeId(fromZoneLink.getId());
			//zone 2 zone
			for (Entry<DgZone, Double> entry : fromZone.getToZoneRelations().entrySet()){
				Id id = createFromZone2ToZoneId(fromZone.getId(), entry.getKey().getId());
				Link toZoneLink = this.zones2LinkMap.get(entry.getKey());
				Id toNodeId = DgMatsim2KoehlerStrehler2010NetworkConverter.convertLinkId2FromCrossingNodeId(toZoneLink.getId());
				this.addCommodity(coms, id, fromNodeId, toNodeId, entry.getValue());
			}
			//zone 2 link
			for (Entry<Link, Double> entry : fromZone.getToLinkRelations().entrySet()){
				Id toNodeId = DgMatsim2KoehlerStrehler2010NetworkConverter.convertLinkId2FromCrossingNodeId(entry.getKey().getId());
				Id id = createFrom2ToId(fromZone.getId(), entry.getKey().getId().toString());
				this.addCommodity(coms, id, fromNodeId, toNodeId, entry.getValue());
			}
			//link 2 x
			for (DgZoneFromLink fromLink : fromZone.getFromLinks().values()){
				Id fromNodeId2 = DgMatsim2KoehlerStrehler2010NetworkConverter.convertLinkId2ToCrossingNodeId(fromLink.getLink().getId());
				//link 2 zone
				for (Entry<DgZone, Double> entry : fromLink.getToZoneRelations().entrySet()){
					Id id = createFrom2ToId(fromLink.getLink().getId().toString(), entry.getKey().getId());
					Link toZoneLink = this.zones2LinkMap.get(entry.getKey());
					Id toNodeId = DgMatsim2KoehlerStrehler2010NetworkConverter.convertLinkId2FromCrossingNodeId(toZoneLink.getId());
					this.addCommodity(coms, id, fromNodeId2, toNodeId, entry.getValue());
				}
				//link 2 link
				for (Entry<Link, Double> entry : fromLink.getToLinkRelations().entrySet()){
					Id id = createFromLink2ToLinkId(fromLink.getLink().getId().toString(), entry.getKey().getId().toString());
					Id toNodeId = DgMatsim2KoehlerStrehler2010NetworkConverter.convertLinkId2FromCrossingNodeId(entry.getKey().getId());
					this.addCommodity(coms, id, fromNodeId2, toNodeId, entry.getValue());
				}
			}			
		}
		return coms;
	}

}
