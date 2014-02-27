package org.matsim.contrib.dvrp.data;

import java.util.Comparator;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;


public class Requests
{
    public static final Comparator<Request> T0_COMPARATOR = new Comparator<Request>() {
        public int compare(Request r1, Request r2)
        {
            return Double.compare(r1.getT0(), r2.getT0());
        }
    };

    public static final Comparator<Request> T1_COMPARATOR = new Comparator<Request>() {
        public int compare(Request r1, Request r2)
        {
            return Double.compare(r1.getT1(), r2.getT1());
        }
    };

    public static final Comparator<Request> SUBMISSION_TIME_COMPARATOR = new Comparator<Request>() {
        public int compare(Request r1, Request r2)
        {
            return Double.compare(r1.getSubmissionTime(), r2.getSubmissionTime());
        }
    };


    public static class IsUrgentPredicate
        implements Predicate<Request>
    {
        private double now;


        public IsUrgentPredicate(double now)
        {
            this.now = now;
        }


        public boolean apply(Request vehicle)
        {
            return isUrgent(vehicle, now);
        }
    }


    public static final boolean isUrgent(Request request, double now)
    {
        return request.getT0() < now;
    }


    public static int countUrgentRequests(Iterable<? extends Request> requests, double now)
    {
        return Iterables.size(Iterables.filter(requests, new IsUrgentPredicate(now)));
    }


    public static <R extends Request> Iterable<R> filterIdleVehicles(Iterable<R> requests,
            double now)
    {
        return Iterables.filter(requests, new IsUrgentPredicate(now));
    }
}
