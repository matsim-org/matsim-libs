/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util;

import org.matsim.contrib.taxi.data.*;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import com.google.inject.Inject;


public class TaxiSimulationConsistencyChecker
    implements AfterMobsimListener
{
    private final TaxiData taxiData;


    @Inject
    public TaxiSimulationConsistencyChecker(TaxiData taxiData)
    {
        this.taxiData = taxiData;
    }


    public void addCheckAllRequestsPerformed()
    {
        for (TaxiRequest r : taxiData.getTaxiRequests().values()) {
            if (r.getStatus() != TaxiRequestStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event)
    {
        addCheckAllRequestsPerformed();
    }
}
