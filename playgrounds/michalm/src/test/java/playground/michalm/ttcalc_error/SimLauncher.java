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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.util.TravelTime;


public class SimLauncher
{
    @SuppressWarnings("deprecation")
    public static void main(String[] args)
    {
        String cfg = "src/test/java/playground/michalm/ttcalc_error/error_1/config.xml";
        // cfg = "src/test/java/playground/michalm/ttcalc_error/error_2/config.xml";

        Controler controler = new Controler(new String[] { cfg });
        controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
        controler.run();

        TravelTime travelTime = controler.getLinkTravelTimes();

        Map<Id<Link>, ? extends Link> links = controler.getScenario().getNetwork().getLinks();
        Id<Link> idB = Id.create("B", Link.class);
        Link linkB = links.get(idB);

        System.out.println("\ndeparture time [min] : travel time [s]\n");

        for (int i = 0; i < 2 * 60 * 60; i += 5 * 60) {// each 5 minutes during the first 2 hours
            int m = i / 60;
            System.out.println(m + " : " + travelTime.getLinkTravelTime(linkB, i, null, null));
        }
    }
}
