package playground.michalm.jtrrouter;

import java.util.List;


/**
 * @author michalm
 */
public class Route
{
    /*package*/final Flow inFlow;
    /*package*/final Flow outFlow;

    /*package*/final int nodeCount;
    /*package*/final String nodes;

    /*package*/final double prob;


    public Route(Flow in, Flow out, List<Integer> nodeList, double prob)
    {
        this.inFlow = in;
        this.outFlow = out;
        this.prob = prob;
        this.nodeCount = nodeList.size();

        StringBuilder sb = new StringBuilder(nodeCount * 3);

        for (int i = 0; i < nodeCount; i++) {
            sb.append(nodeList.get(i)).append(' ');
        }

        nodes = sb.toString();
    }


    public String toString()
    {
        return new StringBuilder(nodes.length() + 15).append("IN").append(inFlow.node).append(' ')
                .append(nodes).append("OUT").append(outFlow.node).toString();
    }


    public Flow getInFlow()
    {
        return inFlow;
    }


    public Flow getOutFlow()
    {
        return outFlow;
    }


    public String getNodes()
    {
        return nodes;
    }


    public int getNodeCount()
    {
        return nodeCount;
    }


    public double getProb()
    {
        return prob;
    }
}
