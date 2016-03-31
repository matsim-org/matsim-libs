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


public class KNTaxiTest
{
    @Test
    public void testRun()
    {
        String configFile = "src/main/resources/mielec_2014_02/KN_config.xml";
        KNTaxi.run(configFile, false, false, false);        
        KNTaxi.run(configFile, true, false, false);
        KNTaxi.run(configFile, true, true, false);
    }
}
