/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.plausibility.log;


import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import playground.polettif.publicTransitMapping.plausibility.PlausibilityCheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public abstract class AbstractPlausibilityWarning implements PlausibilityWarning {

	private static String sep = PlausibilityCheck.CsvSeparator;

	public static final String CSV_HEADER =	"id" + sep +
											"WarningType" + sep +
											"TransitLine" + sep +
											"TransitRoute" + sep +
											"fromId" + sep +
											"toId" + sep +
											"diff" + sep +
											"expected" + sep +
											"actual" + sep +
											"linkIds";

	protected static Network net;
	protected static long idLong = 0;

	protected final String type;

	protected final Id<PlausibilityWarning> id;
	protected final TransitLine transitLine;
	protected final TransitRoute transitRoute;

	protected Tuple<Object, Object> pair;
	protected List<Id<Link>> linkIdList;
	protected String fromId;
	protected String toId;
	protected double expected;
	protected double actual;
	protected double difference;

	public AbstractPlausibilityWarning(String type, TransitLine transitLine, TransitRoute transitRoute) {
		this.id = Id.create(idLong++, PlausibilityWarning.class);
		this.type = type;
		this.transitLine = transitLine;
		this.transitRoute = transitRoute;
	}

	public static void setNetwork(Network network) {
		net = network;
	}

	@Override
	public Id<PlausibilityWarning> getId() {
		return id;
	}

	@Override
	public String getCsvLine() {
		return  id + sep +
				type + sep +
				transitLine.getId() + sep +
				transitRoute.getId() + sep +
				fromId + sep +
				toId + sep +
				difference + sep +
				expected + sep +
				actual + sep +
				CollectionUtils.idSetToString(new HashSet<>(linkIdList));
	}

	@Override
	public List<String> getCsvLineForEachLink() {
		List<String> csvLines = new ArrayList<>();
		for(Id<Link> linkId : linkIdList) {
			String str = getCsvLine()+PlausibilityCheck.CsvSeparator+linkId;
			csvLines.add(str);
		}
		return csvLines;
	}

	@Override
	public Tuple<Object, Object> getPair() {
		return pair;
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		return linkIdList;
	}

	@Override
	public TransitRoute getTransitRoute() {
		return transitRoute;
	}

	@Override
	public TransitLine getTransitLine() {
		return transitLine;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getFromId() {
		return fromId;
	}

	@Override
	public String getToId() {
		return toId;
	}

	@Override
	public double getExpected() {
		return expected;
	}

	@Override
	public double getActual() {
		return actual;
	}

	@Override
	public double getDifference() {
		return difference;
	}

}
