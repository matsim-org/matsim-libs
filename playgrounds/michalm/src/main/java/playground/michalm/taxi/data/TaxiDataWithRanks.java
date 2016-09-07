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
import org.matsim.contrib.taxi.data.TaxiData;


public class TaxiDataWithRanks
    extends TaxiData
{
    private final Map<Id<TaxiRank>, TaxiRank> taxiRanks = new LinkedHashMap<>();

    private final Map<Id<TaxiRank>, TaxiRank> unmodifiableTaxiRanks = Collections
            .unmodifiableMap(taxiRanks);


    public Map<Id<TaxiRank>, TaxiRank> getTaxiRanks()
    {
        return unmodifiableTaxiRanks;
    }


    public void addTaxiRank(TaxiRank taxiRank)
    {
        taxiRanks.put(taxiRank.getId(), taxiRank);
    }
}
