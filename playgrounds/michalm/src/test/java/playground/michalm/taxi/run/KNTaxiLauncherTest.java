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

import org.junit.Test;


public class KNTaxiLauncherTest
{
    @Test
    public void test()
    {
        String dir = "./src/main/resources/mielec-2-peaks_2014_02/";
        String paramFile = dir + "params.in";
        TaxiLauncherParams params = TaxiLauncherParams.readParams(paramFile, dir, dir);
        KNTaxiLauncher.run(params, false, false, false);//equivalent to (file, false, true)        
        KNTaxiLauncher.run(params, true, false, false);
        KNTaxiLauncher.run(params, true, true, false);
    }
}
