/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.ev;

import org.apache.commons.configuration.Configuration;


public class ETaxiParams
{
    public static String CHARGE_TIME_STEP = "chargeTimeStep";
    public static String AUX_DISCHARGE_TIME_STEP = "auxDischargeTimeStep";

    public final int chargeTimeStep;
    public final int auxDischargeTimeStep;

    public ETaxiParams(Configuration config)
    {
        chargeTimeStep = config.getInt(CHARGE_TIME_STEP);
        auxDischargeTimeStep = config.getInt(AUX_DISCHARGE_TIME_STEP);
    }


    //just an example
    public ETaxiParams()
    {
        //no need to simulate with 1-second time step
        chargeTimeStep = 5; //5 s ==> 0.35% SOC (fast charging, 50 kW)
        auxDischargeTimeStep = 60; //1 min ==> 0.25% SOC (3 kW aux power)
    }
}
