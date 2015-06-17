/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.poznan.demand.taxi;

import java.util.Date;

import org.matsim.api.core.v01.*;


public class ServedRequest
{
    public final Id<ServedRequest> id;
    public final Date accepted;
    public final Date assigned;
    public final Coord from;
    public final Coord to;
    public final Id<String> taxiId;


    public ServedRequest(Id<ServedRequest> id, Date accepted, Date assigned, Coord from, Coord to,
            Id<String> taxiId)
    {
        this.id = id;
        this.accepted = accepted;
        this.assigned = assigned;
        this.from = from;
        this.to = to;
        this.taxiId = taxiId;
    }
}