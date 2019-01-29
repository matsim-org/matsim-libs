package org.matsim.contrib.pseudosimulation.distributed;

/**
 * Created by fouriep on 17/7/16.
 *
 * Does nothing for however long you want, so you can bjob-connect in to cluster nodes and test things interactively.
 */
public class Squatter {
    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(1000 * 3600 * Integer.parseInt(args[0]));
    }
}
