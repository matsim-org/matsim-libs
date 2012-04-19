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

package playground.michalm.ttcalc_error;

import org.matsim.contrib.otfvis.OTFVis;


public class SimLauncherOFTVis
{
    public static void main(String[] args)
    {
        String cfg = "src/test/java/playground/michalm/ttcalc_error/error_1/config-OFTVis.xml";
        // cfg = "src/test/java/playground/michalm/ttcalc_error/error_2/config-OFTVis.xml";

        OTFVis.playConfig(cfg);
    }
}
