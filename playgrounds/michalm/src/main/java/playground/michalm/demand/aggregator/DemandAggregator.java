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

package playground.michalm.demand.aggregator;

import java.util.*;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Coord;
import org.matsim.matrices.*;

import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.zone.*;


public class DemandAggregator
{
    private enum TripType
    {
        INTERNAL, INCOMING, OUTGOING, EXTERNAL;
    }


    private final ZoneFinder zoneFinder;
    private final DateDiscretizer dateDiscretizer;
    private final Matrices matrices = new Matrices();

    private final Map<TripType, MutableInt> counters = new EnumMap<>(TripType.class);


    public DemandAggregator(ZoneFinder zoneFinder, DateDiscretizer dateDiscretizer)
    {
        this.zoneFinder = zoneFinder;
        this.dateDiscretizer = dateDiscretizer;

        for (TripType type : TripType.values()) {
            counters.put(type, new MutableInt());
        }
    }


    public void addTrip(Date date, Coord fromCoord, Coord toCoord)
    {
        Zone fromZone = zoneFinder.findZone(fromCoord);
        Zone toZone = zoneFinder.findZone(toCoord);
        TripType type = getTripType(fromZone, toZone);
        counters.get(type).increment();

        if (type == TripType.INTERNAL) {
            addTrip(dateDiscretizer.discretize(date), fromZone.getId().toString(),
                    toZone.getId().toString());
        }
    }


    private TripType getTripType(Zone fromZone, Zone toZone)
    {
        if (fromZone != null) {
            return (toZone != null) ? TripType.INTERNAL : TripType.OUTGOING;
        }
        else {
            return (toZone != null) ? TripType.INCOMING : TripType.EXTERNAL;
        }
    }


    private void addTrip(String key, String fromId, String toId)
    {
        Matrix matrix = MatrixUtils.getOrCreateMatrix(matrices, key);
        MatrixUtils.setOrIncrementValue(matrix, fromId, toId, 1);
    }


    public void printCounters()
    {
        for (TripType type : TripType.values()) {
            System.out.println(type.name() + " trips:\t" + counters.get(type).getValue());
        }
    }


    public Matrices getMatrices()
    {
        return matrices;
    }
}
