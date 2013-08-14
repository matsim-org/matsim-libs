/* *********************************************************************** *
 * project: org.matsim.*
 * EnvelopeLinkStartEndFilter
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
package playground.dgrether;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 *
 */
public class EnvelopeLinkStartEndFilter implements NetworkLinkFilter {

			private Envelope boundingBox;
			
			public EnvelopeLinkStartEndFilter(Envelope envelope) {
				this.boundingBox = envelope;
			}

			@Override
			public boolean judgeLink(Link l) {
				Coord linkStartCoord = l.getFromNode().getCoord();
				Coord linkEndCoord = l.getToNode().getCoord();
				Coordinate linkStartPoint = MGC.coord2Coordinate(linkStartCoord);
				Coordinate linkEndPoint = MGC.coord2Coordinate(linkEndCoord);
				return this.boundingBox.contains(linkStartPoint) || this.boundingBox.contains(linkEndPoint);
			}
}
