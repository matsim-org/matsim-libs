/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF2.ParametersPSF2;

/**
 * @author nagel
 *
 */
public class IntegrationTest2 extends MatsimTestCase {
	
	public void testEventFileBasedOneAgent(){
		performSingleAgentRun();
		
		LinkedList<ChargeLog> chargingTimesForAgent1 = ParametersPSF2.chargingTimes.get(Id.create(1, Person.class)).getChargingTimes();

		assertEquals(2, chargingTimesForAgent1.size());
		// ok when I run the test separately; fails when I run all wrashid playground tests.  kai, dec'15
		
		assertEquals(22500, chargingTimesForAgent1.get(0).getStartChargingTime(),1.0);
		assertEquals(38040, chargingTimesForAgent1.get(1).getStartChargingTime(),1.0);
		
		assertEquals(10*3600*1000.0, chargingTimesForAgent1.getLast().getEndSOC());
	}
	
	private void performSingleAgentRun(){
		PSSControler pssControler=new PSSControlerDumbCharging(getPackageInputDirectory() + "config-event-file-based-oneAgent.xml", null);
		pssControler.runMATSimIterations();
	}

}
