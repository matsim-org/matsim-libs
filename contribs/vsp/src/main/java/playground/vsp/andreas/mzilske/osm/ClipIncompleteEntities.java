/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.vsp.andreas.mzilske.osm;

import org.openstreetmap.osmosis.areafilter.v0_6.AreaFilter;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;

public class ClipIncompleteEntities extends AreaFilter {

	public ClipIncompleteEntities(IdTrackerType idTrackerType,
			boolean clipIncompleteEntities, boolean completeWays,
			boolean completeRelations) {
		super(idTrackerType, true, completeWays, completeRelations, true);
	}

	@Override
	protected boolean isNodeWithinArea(Node arg0) {
		return true;
	}

}
