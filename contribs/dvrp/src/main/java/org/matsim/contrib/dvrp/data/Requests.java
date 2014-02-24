package org.matsim.contrib.dvrp.data;

import java.util.Comparator;


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
}
