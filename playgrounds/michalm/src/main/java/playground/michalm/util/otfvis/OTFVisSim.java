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

package playground.michalm.util.otfvis;

import java.util.Arrays;

import org.matsim.contrib.otfvis.OTFVis;


public class OTFVisSim
{

    public static void main(String[] args)
    {
        String dirName;
        String mviFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            //dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            dirName = "d:\\PP-rad\\taxi\\mielec-2-peaks\\";

            // mviFileName = "output\\config-verB\\ITERS\\it.10\\10.otfvis.mvi";
            //mviFileName = "output\\config-verB\\ITERS\\it.50\\50.otfvis.mvi";

            mviFileName = "20.otfvis.mvi";
        }
        else if (args.length == 2) {
            dirName = args[0];
            mviFileName = args[1];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        OTFVis.playMVI(dirName + mviFileName);
    }
}
