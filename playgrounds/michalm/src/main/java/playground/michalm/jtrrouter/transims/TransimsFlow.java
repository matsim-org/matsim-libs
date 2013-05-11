package playground.michalm.jtrrouter.transims;

import playground.michalm.jtrrouter.Flow;


/**
 * @author michalm
 */
public class TransimsFlow
    extends Flow
{
    /*package*/final int inParking;
    /*package*/final int outParking;

    /*package*/final int[] types;
    /*package*/final int[] subTypes;


    public TransimsFlow(int node, int inParking, int outParking, int next, int[] types,
            int[] subTypes, int[] counts)
    {
        super(node, next, counts, inParking != -1, outParking != -1);

        this.inParking = inParking;
        this.outParking = outParking;

        this.types = types;
        this.subTypes = subTypes;
    }
}
