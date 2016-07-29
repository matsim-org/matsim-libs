/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel.scoring;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping1;
import playground.wrashid.parkingSearch.planLevel.scenario.ParkingUtils;

public class ParkingScoreListenerTest extends MatsimTestCase implements IterationEndsListener {

	public void testScenario() {
		Controler controler;
		String configFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessConfig3.xml";
		final Config config = this.loadConfig(configFilePath);

		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		// too many things don't work with access/egress walk true. kai, jun'16

		controler = new Controler(config);

		ParkingUtils.initializeParking(controler) ;

		ParkingRoot.setParkingScoringFunction(new ParkingScoringFunctionTestNumberOfParkings(new ParkingPriceMapping1(),
				new IncomeRelevantForParking(), null));

		controler.addControlerListener(this);

		controler.run();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		HashMap<Id<Person>, Double> hm = ParkingScoreExecutor.getScoreHashMap();
		assertEquals(-2.0, hm.get(Id.create(1, Person.class)).doubleValue());
		assertEquals(-2.0, hm.get(Id.create(2, Person.class)).doubleValue());
		assertEquals(-2.0, hm.get(Id.create(3, Person.class)).doubleValue());
	}

}
