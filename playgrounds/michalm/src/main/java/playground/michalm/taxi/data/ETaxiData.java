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

package playground.michalm.taxi.data;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.taxi.data.*;

import playground.michalm.ev.Charger;


public class ETaxiData
    extends TaxiData
{
    private final Map<Id<TaxiRank>, TaxiRank> taxiRanks = new LinkedHashMap<>();
    private final Map<Id<Charger>, Charger> chargers = new LinkedHashMap<>();

    private final Map<Id<TaxiRank>, TaxiRank> unmodifiableTaxiRanks = Collections
            .unmodifiableMap(taxiRanks);
    private final Map<Id<Charger>, Charger> unmodifiableChargers = Collections
            .unmodifiableMap(chargers);


    public Map<Id<TaxiRank>, TaxiRank> getTaxiRanks()
    {
        return unmodifiableTaxiRanks;
    }


    public Map<Id<Charger>, Charger> getChargers()
    {
        return unmodifiableChargers;
    }


    public Map<Id<Vehicle>, ETaxi> getETaxis()
    {
        return convertMap(getVehicles());
    }


    public Map<Id<Request>, TaxiRequest> getTaxiRequests()
    {
        return convertMap(getRequests());
    }


    public void addTaxiRank(TaxiRank taxiRank)
    {
        taxiRanks.put(taxiRank.getId(), taxiRank);
    }


    public void addCharger(Charger charger)
    {
        chargers.put(charger.getId(), charger);
    }


    //casts Collection of supertype S to Collection of type T
    @SuppressWarnings("unchecked")
    private static <I, S, T> Map<I, T> convertMap(Map<I, S> collection)
    {
        return (Map<I, T>)collection;
    }
}
