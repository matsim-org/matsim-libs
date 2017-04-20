/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.data.file;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.file.ReaderUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.michalm.taxi.data.*;

public class TaxiRankReader extends MatsimXmlParser {
	private final static String RANK = "rank";

	private final static int DEFAULT_RANK_CAPACITY = Integer.MAX_VALUE;

	private final TaxiRankDataImpl data;
	private Map<Id<Link>, ? extends Link> links;

	public TaxiRankReader(Network network, TaxiRankDataImpl data) {
		this.data = data;
		links = network.getLinks();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (RANK.equals(name)) {
			data.addTaxiRank(createRank(atts));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private TaxiRank createRank(Attributes atts) {
		Id<TaxiRank> id = Id.create(atts.getValue("id"), TaxiRank.class);
		String name = ReaderUtils.getString(atts, "name", id + "");
		Link link = links.get(Id.createLinkId(atts.getValue("link")));
		int capacity = ReaderUtils.getInt(atts, "capacity", DEFAULT_RANK_CAPACITY);
		return new TaxiRank(id, name, link, capacity);
	}
}
