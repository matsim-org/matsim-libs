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

import org.matsim.contrib.dvrp.data.VrpDataImpl;

import playground.michalm.ev.Charger;


public class ETaxiData
    extends VrpDataImpl
{
    private final Collection<TaxiRank> taxiRanks = new ArrayList<>();
    private final Collection<Charger> chargers = new ArrayList<>();

    private final Collection<TaxiRank> unmodifiableTaxiRanks = Collections.unmodifiableCollection(taxiRanks);
    private final Collection<Charger> unmodifiableChargers = Collections.unmodifiableCollection(chargers);


    public Collection<TaxiRank> getTaxiRanks()
    {
        return unmodifiableTaxiRanks;
    }


    public Collection<Charger> getChargers()
    {
        return unmodifiableChargers;
    }


    public Collection<ETaxi> getETaxis()
    {
        return convertCollection(getVehicles());
    }


    public Collection<TaxiRequest> getTaxiRequests()
    {
        return convertCollection(getRequests());
    }


    public void addTaxiRank(TaxiRank taxiRank)
    {
        taxiRanks.add(taxiRank);
    }


    public void addCharger(Charger charger)
    {
        chargers.add(charger);
    }


    //casts Collection of supertype S to Collection of type T
    @SuppressWarnings("unchecked")
    private static <S, T> Collection<T> convertCollection(Collection<S> collection)
    {
        return (Collection<T>)collection;
    }
}
