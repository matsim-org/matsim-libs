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
    private final List<TaxiRank> taxiRanks = new ArrayList<>();
    private final List<Charger> chargers = new ArrayList<>();

    private final List<TaxiRank> unmodifiableTaxiRanks = Collections.unmodifiableList(taxiRanks);
    private final List<Charger> unmodifiableChargers = Collections.unmodifiableList(chargers);


    public List<TaxiRank> getTaxiRanks()
    {
        return unmodifiableTaxiRanks;
    }


    public List<Charger> getChargers()
    {
        return unmodifiableChargers;
    }


    public List<ETaxi> getETaxis()
    {
        return convertList(getVehicles());
    }


    public List<TaxiRequest> getTaxiRequests()
    {
        return convertList(getRequests());
    }


    public void addTaxiRank(TaxiRank taxiRank)
    {
        taxiRanks.add(taxiRank);
    }


    public void addCharger(Charger charger)
    {
        chargers.add(charger);
    }


    //casts List of supertype S to List of type T
    @SuppressWarnings("unchecked")
    private static <S, T> List<T> convertList(List<S> list)
    {
        return (List<T>)list;
    }
}
