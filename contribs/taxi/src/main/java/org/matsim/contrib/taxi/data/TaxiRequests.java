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

package org.matsim.contrib.taxi.data;

import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;


public class TaxiRequests
{
    public static final Predicate<TaxiRequest> IS_UNPLANNED = new TaxiRequestStatusPredicate(
            TaxiRequestStatus.UNPLANNED);

    public static final Predicate<TaxiRequest> IS_PLANNED = new TaxiRequestStatusPredicate(
            TaxiRequestStatus.PLANNED);

    public static final Predicate<TaxiRequest> IS_TAXI_DISPATCHED = new TaxiRequestStatusPredicate(
            TaxiRequestStatus.TAXI_DISPATCHED);

    public static final Predicate<TaxiRequest> IS_PICKUP = new TaxiRequestStatusPredicate(
            TaxiRequestStatus.PICKUP);

    public static final Predicate<TaxiRequest> IS_RIDE = new TaxiRequestStatusPredicate(
            TaxiRequestStatus.RIDE);

    public static final Predicate<TaxiRequest> IS_DROPOFF = new TaxiRequestStatusPredicate(
            TaxiRequestStatus.DROPOFF);

    public static final Predicate<TaxiRequest> IS_PERFORMED = new TaxiRequestStatusPredicate(
            TaxiRequestStatus.PERFORMED);


    public static class TaxiRequestStatusPredicate
        implements Predicate<TaxiRequest>
    {
        private final TaxiRequestStatus status;


        public TaxiRequestStatusPredicate(TaxiRequestStatus status)
        {
            this.status = status;
        }


        @Override
        public boolean apply(TaxiRequest r)
        {
            return r.getStatus() == status;
        }
    }


    public static int countRequestsWithStatus(Iterable<TaxiRequest> requests,
            TaxiRequestStatus status)
    {
        return Iterables.size(Iterables.filter(requests, new TaxiRequestStatusPredicate(status)));
    }
}
