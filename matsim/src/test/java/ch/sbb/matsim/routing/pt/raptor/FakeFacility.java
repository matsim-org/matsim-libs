/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.Facility;

import java.util.Map;

/**
 * @author mrieser / SBB
 */
public final class FakeFacility implements Facility {
    private final Coord coord;
    private final Id<Link> linkId;

    public FakeFacility(Coord coord) {
        this(coord, null);
    }

    FakeFacility(Coord coord, Id<Link> linkId) {
        this.coord = coord;
        this.linkId = linkId;
    }

    @Override
	public Coord getCoord() {
        return this.coord;
    }

    public Id getId() {
        throw new RuntimeException("not implemented");
    }

    @Override
	public Map<String, Object> getCustomAttributes() {
        throw new RuntimeException("not implemented");
    }

    @Override
	public Id getLinkId() {
        return this.linkId;
    }
}
