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

package playground.michalm.util.sim;

import java.util.Arrays;

import org.matsim.core.controler.Controler;


public class SimLauncher
{
    @SuppressWarnings("deprecation")
    public static void main(String[] args)
    {
        String dirName;
        String cfgFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "d:/PP-rad/taxi/mielec-2-peaks/2013_02/input/";
            cfgFileName = "siec-config.xml";
            // dirName = "d:\\PP-rad\\taxi\\poznan\\";
            // cfgFileName = "poznan-config.xml";
        }
        else if (args.length == 2) {
            dirName = args[0];
            cfgFileName = args[1];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Controler controler = new Controler(new String[] { dirName + cfgFileName });
        controler.setOverwriteFiles(true);
        controler.run();
    }
}
