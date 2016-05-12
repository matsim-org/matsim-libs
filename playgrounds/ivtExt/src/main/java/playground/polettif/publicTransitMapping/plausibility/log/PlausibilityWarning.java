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
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.List;

public interface PlausibilityWarning extends Identifiable<PlausibilityWarning> {

	String getType();

	List<Id<Link>> getLinkIds();

	TransitLine getTransitLine();

	TransitRoute getTransitRoute();

	String getFromId();

	String getToId();

	double getExpected();

	double getActual();

	double getDifference();

	Tuple<Object,Object> getPair();

	String getCsvLine();

	List<String> getCsvLineForEachLink();
}
