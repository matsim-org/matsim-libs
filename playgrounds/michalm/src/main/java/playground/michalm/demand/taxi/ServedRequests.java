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

package playground.michalm.demand.taxi;

import java.util.*;

import org.matsim.core.utils.geometry.geotools.MGC;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;


public class ServedRequests
{
    public enum WeekDay
    {
        SUN, MON, TUE, WED, THU, FRI, SAT;//weird ordering imposed by Date

        public static WeekDay getWeekDay(Date date)
        {
            return WeekDay.values()[date.getDay()];
        }
    }


    public static boolean isWithinArea(ServedRequest request, PreparedPolygon preparedPolygon)
    {
        Point from = MGC.coord2Point(request.getFrom());
        Point to = MGC.coord2Point(request.getTo());
        return preparedPolygon.contains(from) && preparedPolygon.contains(to);
    }


    public static boolean isBetweenDates(ServedRequest request, Date fromDate, Date toDate)
    {
        long assignedTime = request.getStartTime().getTime();
        return assignedTime >= fromDate.getTime() && assignedTime < toDate.getTime();
    }


    public static boolean isOnWeekDays(ServedRequest request, WeekDay... weekDays)
    {
        WeekDay wd = WeekDay.getWeekDay(request.getStartTime());
        return Arrays.asList(weekDays).contains(wd);
    }


    public static Predicate<ServedRequest> createWithinAreaPredicate(MultiPolygon area)
    {
        final PreparedPolygon preparedPolygon = new PreparedPolygon(area);
        return new Predicate<ServedRequest>() {
            public boolean apply(ServedRequest request)
            {
                return isWithinArea(request, preparedPolygon);
            }
        };
    }


    public static Predicate<ServedRequest> createBetweenDatesPredicate(final Date fromDate,
            final Date toDate)
    {
        return new Predicate<ServedRequest>() {
            public boolean apply(ServedRequest request)
            {
                return ServedRequests.isBetweenDates(request, fromDate, toDate);
            }
        };
    }


    public static Predicate<ServedRequest> createOnWeekDaysPredicate(final WeekDay... weekDays)
    {
        return new Predicate<ServedRequest>() {
            public boolean apply(ServedRequest request)
            {
                return ServedRequests.isOnWeekDays(request, weekDays);
            }
        };
    }


    public static <T extends ServedRequest> Iterable<T> filterWorkDaysPeriods(Iterable<T> requests,
            final int zeroHour)
    {
        Predicate<ServedRequest> predicate = new Predicate<ServedRequest>() {
            public boolean apply(ServedRequest request)
            {
                WeekDay wd = WeekDay.getWeekDay(request.getStartTime());

                switch (wd) {
                    case MON:
                        return request.getStartTime().getHours() >= zeroHour;

                    case TUE:
                    case WED:
                    case THU:
                        return true;

                    case SAT:
                    case SUN:
                        return false;

                    case FRI:
                        return request.getStartTime().getHours() < zeroHour;

                    default:
                        throw new IllegalArgumentException();
                }
            }
        };

        return Iterables.filter(requests, predicate);
    }
}
