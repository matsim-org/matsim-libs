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

package playground.michalm.barcelona.demand;

import java.util.*;

import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.MultiPolygon;

import playground.michalm.barcelona.BarcelonaZones;
import playground.michalm.demand.taxi.ServedRequests;


public class BarcelonaServedRequests
{
    public static final int ZERO_HOUR = 5;

    
    public static List<BarcelonaServedRequest> readRequests()
    {
        List<BarcelonaServedRequest> requests = new ArrayList<>();
        String file = "d:/PP-rad/Barcelona/data/served_requests/tripsBCN.csv";
        new BarcelonaServedRequestsReader(requests).readFile(file);
        return requests;
    }


    public static Iterable<BarcelonaServedRequest> filterRequestsWithinAgglomeration(
            Iterable<BarcelonaServedRequest> requests)
    {
        MultiPolygon area = BarcelonaZones.readAgglomerationArea();
        return Iterables.filter(requests, ServedRequests.createWithinAreaPredicate(area));
    }


    public static Iterable<BarcelonaServedRequest> filterFromMar2011(
            Iterable<BarcelonaServedRequest> requests)
    {
        //01/03-1/11/2011
        @SuppressWarnings("unchecked")
        Predicate<BarcelonaServedRequest> orPredicate = Predicates.or(
                ServedRequests.createBetweenDatesPredicate(midnight("01/03/2011"), midnight("1/12/2014")));

        return Iterables.filter(requests, orPredicate);
    }


    private static Date midnight(String date)
    {
        //format: "dd/MM/yyyy HH:mm"
        return BarcelonaServedRequestsReader.parseDate(date + " 00:00:00");
    }
}
