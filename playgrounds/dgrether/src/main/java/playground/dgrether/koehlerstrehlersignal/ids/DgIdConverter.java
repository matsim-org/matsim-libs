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

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;


/**
 * @author dgrether
 *
 */
public class DgIdConverter {

	private DgIdPool idPool;
	
	public DgIdConverter(DgIdPool idpool){
		this.idPool = idpool;
	}
	
	public  Id convertLinkId2FromCrossingNodeId(Id linkId){
		String idString = linkId.toString() + "11";
//		String idString = linkId.toString();
		return idPool.createId(idString);
	}
	
	public  Id convertLinkId2ToCrossingNodeId(Id linkId){
		String idString = linkId.toString() + "99";
		return idPool.createId(idString);
	}
	
	public  Id convertFromLinkIdToLinkId2LightId(Id fromLinkId, Id fromLaneId, Id toLinkId){
		Id id =  null;
		if (fromLaneId == null){
			id = new IdImpl(fromLinkId.toString()  + "55" + toLinkId.toString());
		}
		else {
			id = new IdImpl(fromLinkId.toString() + "66" + fromLaneId.toString() + "55" + toLinkId.toString());
		}
		String idString = id.toString();
		return idPool.createId(idString);
	}
	
	public  Id convertNodeId2CrossingId(Id nodeId){
		String idString = nodeId.toString() + "77";
		return idPool.createId(idString);
	}
	
	public  Id convertLinkId2StreetId(Id linkId){
		String idString = linkId.toString() + "88";
		return idPool.createId(idString);
	}

	public Id createFromZone2ToZoneId(Id from, Id to){
		String idString = from + "22" + to;
		return idPool.createId(idString);
	}

	public Id createFromLink2ToLinkId(Id from, Id to){
		String idString = from + "33" + to;
		return idPool.createId(idString);
	}
	
	public Id createFrom2ToId(Id from, Id to){
		String idString = from + "44" + to;
		return idPool.createId(idString);
	}

	
	
}
