package playground.michalm.jtrrouter.matsim;

import playground.michalm.jtrrouter.*;


/**
 * @author michalm
 */
public class MATSimFlow
    extends Flow
{
    /*package*/final int inLink;
    /*package*/final int outLink;


    public MATSimFlow(int node, int inLink, int outLink, int next, int count)
    {
        super(node, next, new int[] { count }, inLink != -1, outLink != -1);

        this.inLink = inLink;
        this.outLink = outLink;
    }


    public int getInLink()
    {
        return inLink;
    }


    public int getOutLink()
    {
        return outLink;
    }
}
