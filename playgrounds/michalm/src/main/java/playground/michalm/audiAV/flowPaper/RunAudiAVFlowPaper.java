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

package playground.michalm.audiAV.flowPaper;

import org.matsim.contrib.dvrp.trafficmonitoring.*;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.router.util.TravelTime;


public class RunAudiAVFlowPaper
{
    public static void run(String configFile, String inputEvents)
    {
        Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup());
        final Controler controler = RunTaxiScenario.createControler(config, false);

        final TravelTime initialTT = TravelTimeUtils
                .createTravelTimesFromEvents(controler.getScenario(), inputEvents);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install()
            {
//XXX this line is used in the 'flowpaper' (branch 'increasedFlowCapForAVs')
//                bind(VehicleType.class).annotatedWith(Names.named(TaxiModule.TAXI_MODE))
//                        .toInstance(VehicleUtils.AUTONOMOUS_VEHICLE_TYPE);

                bind(VrpTravelTimeEstimator.Params.class).toInstance(
                        new VrpTravelTimeEstimator.Params(initialTT, 0.05));
            }
        });

        controler.run();
    }


    public static void main(String[] args)
    {
//XXX this line is used in the 'flowpaper' (branch 'increasedFlowCapForAVs')
//        VehicleUtils.avFlowFactor = Double.parseDouble(args[1]);
//        System.out.println("VehicleUtils.avFlowFactor = " + VehicleUtils.avFlowFactor);
        run(args[0], args[2]);
    }
}
