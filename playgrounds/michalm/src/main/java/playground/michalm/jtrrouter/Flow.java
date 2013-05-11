package playground.michalm.jtrrouter;

import java.util.*;


/**
 * @author michalm
 */
public class Flow
{
    /*package*/final int node;
    /*package*/final int next;

    /*package*/final int[] counts;

    /*package*/final boolean isInFlow;
    /*package*/final boolean isOutFlow;

    /*package*/final List<Route> routes;// routes found


    public Flow(int node, int next, int[] counts, boolean isInFlow, boolean isOutFlow)
    {
        this.node = node;
        this.next = next;

        this.counts = counts;

        this.isInFlow = isInFlow;
        this.isOutFlow = isOutFlow;

        routes = new ArrayList<Route>();
    }
}
