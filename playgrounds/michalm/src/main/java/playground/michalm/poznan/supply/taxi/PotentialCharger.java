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

package playground.michalm.poznan.supply.taxi;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.geometry.CoordImpl;


public class PotentialCharger
    implements BasicLocation<PotentialCharger>
{
    public static final PotentialCharger[] CHARGERS = {
            create(1, 629375.526287495, 5807049.09624582),
            create(2, 627667.270111601, 5806032.00666041),
            create(3, 629858.161036597, 5807986.09770554),
            create(4, 627670.750480261, 5808514.98110016),
            create(5, 628931.787064244, 5808428.14220098),
            create(6, 632665.914282043, 5806912.26666199),
            create(7, 631414.841065603, 5808351.76701608),
            create(8, 631350.921863240, 5807870.60505818),
            create(9, 631357.194136764, 5810714.22475246),
            create(10, 631256.667354371, 5811489.93034689),
            create(11, 630810.948612502, 5812932.31900434),
            create(12, 630740.137462418, 5806467.83836639) //
    };


    private static PotentialCharger create(long id, double x, double y)
    {
        return new PotentialCharger(Id.create(id, PotentialCharger.class), new CoordImpl(x, y));
    }


    private final Id<PotentialCharger> id;
    private final Coord coord;


    public PotentialCharger(Id<PotentialCharger> id, Coord coord)
    {
        this.id = id;
        this.coord = coord;
    }


    @Override
    public Id<PotentialCharger> getId()
    {
        return id;
    }


    @Override
    public Coord getCoord()
    {
        return coord;
    }
}
