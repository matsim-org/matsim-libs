/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.chargerlocation;

import org.matsim.api.core.v01.*;


public class ChargerLocation
    implements BasicLocation<ChargerLocation>
{
    private final Id<ChargerLocation> id;
    private final Coord coord;
    private final double power;


    public ChargerLocation(Id<ChargerLocation> id, Coord coord, double power)
    {
        this.id = id;
        this.coord = coord;
        this.power = power;
    }


    @Override
    public Id<ChargerLocation> getId()
    {
        return id;
    }


    @Override
    public Coord getCoord()
    {
        return coord;
    }


    public double getPower()
    {
        return power;
    }
}
