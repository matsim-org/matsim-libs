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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgNetwork;
import playground.dgrether.utils.DgZone;


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

	@Override
	public DgCommodities convert(ScenarioImpl sc, DgNetwork dgNetwork) {
		DgCommodities coms = new DgCommodities();
		for (DgZone fromZone : this.zones2LinkMap.keySet()){
			Link fromZoneLink = this.zones2LinkMap.get(fromZone);
			for (DgZone toZone : fromZone.getToRelationships().keySet()){
				DgCommodity com = new DgCommodity(new IdImpl(fromZone.getId() + "_" + toZone.getId()));
					coms.addCommodity(com);
				Link toZoneLink = this.zones2LinkMap.get(toZone);
				//TODO check translation of demand again (ids and flow)
				com.addSourceNode(fromZoneLink.getToNode().getId(), fromZone.getToRelationships().get(toZone).doubleValue());
				com.addDrainNode(toZoneLink.getToNode().getId());
			}
		}
		return coms;
	}

}
