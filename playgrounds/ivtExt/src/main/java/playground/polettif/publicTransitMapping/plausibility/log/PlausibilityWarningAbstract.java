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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import playground.polettif.publicTransitMapping.plausibility.PlausibilityCheck;

import java.util.ArrayList;
import java.util.List;


public abstract class PlausibilityWarningAbstract implements PlausibilityWarning {

	protected static Network net;

	protected final int msgOrder;
	protected final String type;

	protected final TransitLine transitLine;
	protected final TransitRoute transitRoute;

	protected Tuple<Object, Object> pair;
	protected List<Id<Link>> linkList;
	protected Coordinate[] coordinates;
	protected String fromId;
	protected String toId;
	protected double expected;
	protected double actual;
	protected double difference;

	public PlausibilityWarningAbstract(int order, String type, TransitLine transitLine, TransitRoute transitRoute) {
		this.msgOrder = order;
		this.type = type;
		this.transitLine = transitLine;
		this.transitRoute = transitRoute;
	}

	public static void setNetwork(Network network) {
		net = network;
	}

	@Override
	public String getCsvLine() {
		return type + PlausibilityCheck.CsvSeparator +
				transitLine.getId() + PlausibilityCheck.CsvSeparator +
				transitRoute.getId() + PlausibilityCheck.CsvSeparator +
				fromId + PlausibilityCheck.CsvSeparator +
				toId + PlausibilityCheck.CsvSeparator +
				difference + PlausibilityCheck.CsvSeparator +
				expected + PlausibilityCheck.CsvSeparator +
				actual;
	}

	@Override
	public List<String> getCsvLineForEachLink() {
		List<String> csvLines = new ArrayList<>();
		for(Id<Link> linkId : linkList) {
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
		return linkList;
	}

	@Override
	public Coordinate[] getCoordinates() {
		return coordinates;
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
	public int getOrder() {
		return msgOrder;
	}

	@Override
	public int compareTo(PlausibilityWarning o) {
		int r = this.getOrder()-o.getOrder();
		return (r == 0 ? -1 : r);
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
