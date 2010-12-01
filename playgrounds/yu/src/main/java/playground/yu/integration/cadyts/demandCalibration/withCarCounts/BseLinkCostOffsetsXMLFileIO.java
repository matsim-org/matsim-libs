/* *********************************************************************** *
 * project: org.matsim.*
 * BseLinkCostOffsetsXMLFileIO.java
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.xml.sax.Attributes;

import cadyts.utilities.misc.DynamicData;
import cadyts.utilities.misc.DynamicDataXMLFileIO;

/** @author yu */
public class BseLinkCostOffsetsXMLFileIO extends DynamicDataXMLFileIO<Link> {
	/***/
	private static final long serialVersionUID = 1L;
	private Network net;

	public BseLinkCostOffsetsXMLFileIO(Network net) {
		super();
		this.net = net;
	}

	@Override
	protected Link attrValue2key(String linkId) {
		System.out.println("-----attrValue2key------:\t" + linkId);
		Link link = net.getLinks().get(new IdImpl(linkId));
		System.out.println("link :\t" + link);
		return link;
	}

	@Override
	protected String key2attrValue(Link key) {
		return key.getId().toString();
	}

	@Override
	protected DynamicData<Link> newInstance(int startTime_s, int binSize_s,
			int binCnt) {
		return new DynamicData<Link>(startTime_s, binSize_s, binCnt);
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) {

		// StringBuilder sb = new StringBuilder();
		// for (int i = 0; i < attributes.getLength(); i++) {
		// sb.append("\t" + attributes.getQName(i) + "\t"
		// + attributes.getValue(i));
		// }
		// System.out.println("element :\t" + name + "\t" + sb);
		super.startElement(uri, localName, name, attributes);
	}
}
