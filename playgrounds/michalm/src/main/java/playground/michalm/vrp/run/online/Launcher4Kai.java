/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.run.online;

import java.io.IOException;


public class Launcher4Kai
{
    public static void main(String[] args)
        throws IOException
    {
        String[] arguments = new String[5];
        // PATHS
        arguments[0] = "../../maciejewski/input/test/taxi_single_iteration/grid-net";
        arguments[1] = "network.xml";
        arguments[2] = "plans.xml";
        arguments[3] = "depots.xml";

        // OFTVis on?
        arguments[4] = "true";

        SingleIterOnlineDvrpLauncher.main(arguments);
    }
}
