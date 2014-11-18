/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

public class BerlinTaxiLauncher
{
    public static void main(String[] args)
    {
        String file = "d:/michalm/eclipse-vsp/eCab/scenarios/2014_10_basic_scenario_v4/params.in";
        //String file = "d:/eclipse-vsp/sustainability-w-michal-and-dlr/data/scenarios/2014_10_basic_scenario_v4/params.in";
        TaxiLauncher launcher = new TaxiLauncher(TaxiLauncherParams.readParams(file));
        launcher.initVrpPathCalculator();
        launcher.go(false);
        launcher.generateOutput();
    }
}
