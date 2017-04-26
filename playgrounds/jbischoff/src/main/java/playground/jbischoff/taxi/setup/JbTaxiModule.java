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

package playground.jbischoff.taxi.setup;

import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;


public class JbTaxiModule
    extends AbstractModule
{
    @Override
    public void install()
    {
		bind(SubmittedTaxiRequestsCollector.class).toInstance(new SubmittedTaxiRequestsCollector());

        //addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
//        addControlerListenerBinding().to(TaxiStatsDumper.class);

        if (TaxiConfigGroup.get(getConfig()).getTimeProfiles()) {
            addMobsimListenerBinding().toProvider(TaxiStatusTimeProfileCollectorProvider.class);
            //add more time profiles if necessary
        }
    }
}
