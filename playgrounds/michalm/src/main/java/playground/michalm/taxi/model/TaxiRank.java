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

package playground.michalm.taxi.model;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;


public class TaxiRank
    implements BasicLocation
{
    private final Id id;
    private final String name;
    private final Link link;


    public TaxiRank(Id id, String name, Link link)
    {
        this.id = id;
        this.name = name;
        this.link = link;
    }


    @Override
    public Id getId()
    {
        return id;
    }


    @Override
    public Coord getCoord()
    {
        return link.getCoord();
    }


    public String getName()
    {
        return name;
    }


    public Link getLink()
    {
        return link;
    }
}
