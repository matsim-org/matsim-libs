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

package playground.michalm.taxi;

import java.util.*;

import org.matsim.contrib.dvrp.passenger.PassengerCustomer;

import pl.poznan.put.vrp.dynamic.data.VrpDataImpl;
import pl.poznan.put.vrp.dynamic.extensions.electric.*;
import playground.michalm.taxi.model.*;


public class TaxiData
    extends VrpDataImpl
    implements ElectricVrpData
{
    private final List<Charger> chargers = new ArrayList<Charger>();


    public List<Charger> getChargers()
    {
        return chargers;
    }


    public void addCharger(Charger charger)
    {
        chargers.add(charger);
    }


    public List<VrpAgentElectricTaxi> getElectricTaxis()
    {
        return convertList(getVehicles());
    }


    public List<TaxiRequest> getTaxiRequests()
    {
        return convertList(getRequests());
    }


    public List<PassengerCustomer> getPassengerCustomers()
    {
        return convertList(getCustomers());
    }


    //casts List of supertype S to List of type T
    @SuppressWarnings("unchecked")
    private static <S, T> List<T> convertList(List<S> list)
    {
        return (List<T>)list;
    }
}
