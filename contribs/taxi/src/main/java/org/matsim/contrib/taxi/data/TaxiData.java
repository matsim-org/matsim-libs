/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.taxi.data;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.*;


public class TaxiData
    extends VrpDataImpl
{
//    private final Map<Id<TaxiRank>, TaxiRank> taxiRanks = new LinkedHashMap<>();

//    private final Map<Id<TaxiRank>, TaxiRank> unmodifiableTaxiRanks = Collections
//            .unmodifiableMap(taxiRanks);


//    public Map<Id<TaxiRank>, TaxiRank> getTaxiRanks()
//    {
//        return unmodifiableTaxiRanks;
//    }


//    public Map<Id<Vehicle>, Taxi> getTaxis()
//    {
//        return convertMap(getVehicles());
//    }


    public Map<Id<Request>, TaxiRequest> getTaxiRequests()
    {
        return convertMap(getRequests());
    }


//    public void addTaxiRank(TaxiRank taxiRank)
//    {
//        taxiRanks.put(taxiRank.getId(), taxiRank);
//    }


    //casts Collection of supertype S to Collection of type T
    @SuppressWarnings("unchecked")
    private static <I, S, T> Map<I, T> convertMap(Map<I, S> collection)
    {
        return (Map<I, T>)collection;
    }
}
