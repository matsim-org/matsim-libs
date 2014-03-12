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

package playground.michalm.taxi.data;

import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;

import com.google.common.base.Predicate;


public class TaxiRequests
{
    public static final Predicate<TaxiRequest> IS_PLANNED = new Predicate<TaxiRequest>() {
        public boolean apply(TaxiRequest r)
        {
            return r.getStatus() == TaxiRequestStatus.PLANNED;
        };
    };
}
