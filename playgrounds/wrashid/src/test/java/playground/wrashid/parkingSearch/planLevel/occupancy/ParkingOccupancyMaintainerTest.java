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

package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

public class ParkingOccupancyMaintainerTest extends MatsimTestCase implements ShutdownListener{

	ParkingBookKeeper parkingBookKeeper=null;

	public void testBasic(){
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig3.xml";
		controler = new Controler(this.loadConfig(configFilePath));

		BaseControlerScenario bs= new BaseControlerScenario(controler);
		parkingBookKeeper=bs.parkingBookKeeper;

		controler.addControlerListener(this);

		controler.run();
	}

	/**
	 * add test just before shutdown of the system.
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		ParkingOccupancyBins pob=parkingBookKeeper.getParkingOccupancyMaintainer().getParkingOccupancyBins().get(new IdImpl(36));

		assertEquals(3, pob.getOccupancy(38000));

		pob=parkingBookKeeper.getParkingOccupancyMaintainer().getParkingOccupancyBins().get(new IdImpl(1));
		assertEquals(3, pob.getOccupancy(0));

		ParkingCapacityFullLogger pcfl=parkingBookKeeper.getParkingOccupancyMaintainer().getParkingCapacityFullTimes().get(new IdImpl(36));
		assertEquals(true, pcfl.isParkingFullAtTime(38000));

		assertEquals(false, pcfl.isParkingFullAtTime(0));

		pcfl=parkingBookKeeper.getParkingOccupancyMaintainer().getParkingCapacityFullTimes().get(new IdImpl(1));

		assertEquals(true, pcfl.isParkingFullAtTime(0));

		assertEquals(false, pcfl.isParkingFullAtTime(38000));


		ParkingArrivalDepartureLog pal=parkingBookKeeper.getParkingOccupancyMaintainer().getParkingArrivalDepartureLog().get(new IdImpl(1));

		assertEquals(2, pal.getParkingArrivalDepartureList().size());


	}



}
