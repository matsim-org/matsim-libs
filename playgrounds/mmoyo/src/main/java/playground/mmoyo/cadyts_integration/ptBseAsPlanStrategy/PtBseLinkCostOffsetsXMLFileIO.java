package playground.mmoyo.w_ptCounts_from_kai.ptBseAsPlanStrategy;

/* *********************************************************************** *
 * project: org.matsim.*
 * PtBseLinkCostOffsetsXMLFileIO.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */

//LINK import org.matsim.api.core.v01.network.Link;
//LINK import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.utilities.misc.DynamicDataXMLFileIO;

/** @author yu */
class PtBseLinkCostOffsetsXMLFileIO extends DynamicDataXMLFileIO</*Link*/TransitStopFacility> {
	/***/
	private static final long serialVersionUID = 1L;
//LINK	private Network net;
	private TransitSchedule schedule; 
	
	PtBseLinkCostOffsetsXMLFileIO(TransitSchedule schedule) {
		super();
		this.schedule = schedule;
	}
	
//LINK	
	/*	
	PtBseLinkCostOffsetsXMLFileIO(Network net) {
		super();
		this.net = net;
	}
 
//LINK
 	@Override
	protected Link attrValue2key(String linkId) {
		System.out.println("-----attrValue2key------:\t" + linkId);
		Link link = net.getLinks().get(new IdImpl(linkId));
		System.out.println("link :\t" + link);
		return link;
	}
	*/

	String strAttrValue2key = "-----attrValue2key------:\t";
	String strStop = "stop :\t";
	@Override
	protected TransitStopFacility attrValue2key(String stopId) {
		System.out.println(strAttrValue2key + stopId);
		TransitStopFacility stop = this.schedule.getFacilities().get(new IdImpl(stopId));
		System.out.println(strStop + stop);
		return stop;
	}
	
	@Override
	protected String key2attrValue(/*Link*/TransitStopFacility key) {
		return key.getId().toString();
	}

//	@Override
//	protected DynamicData<Link> newInstance(int startTime_s, int binSize_s, int binCnt) {
//		return new DynamicData<Link>(startTime_s, binSize_s, binCnt);
//	}

//	@Override
//	public void startElement(String uri, String localName, String name, Attributes attributes) {
//
//		// StringBuilder sb = new StringBuilder();
//		// for (int i = 0; i < attributes.getLength(); i++) {
//		// sb.append("\t" + attributes.getQName(i) + "\t"
//		// + attributes.getValue(i));
//		// }
//		// System.out.println("element :\t" + name + "\t" + sb);
//		super.startElement(uri, localName, name, attributes);
//	}

}
