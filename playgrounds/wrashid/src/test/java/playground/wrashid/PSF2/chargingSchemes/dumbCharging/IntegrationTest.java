/* *********************************************************************** *
 * project: org.matsim.*
 * IntegrationTest.java
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

package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF2.ParametersPSF2;

public class IntegrationTest extends MatsimTestCase {

	public void testBasic(){
		LinkedList<String> allowedChargingLocations=new LinkedList<String>();
		
		allowedChargingLocations.add("home");
		allowedChargingLocations.add("work");
		
		ParametersPSF2.setAllowedChargingLocations(allowedChargingLocations);
		
		PSSControler pssControler=new PSSControlerDumbCharging( getPackageInputDirectory() + "config.xml", null);
		
		pssControler.getConfig().plansCalcRoute().setInsertingAccessEgressWalk(false);
		
		pssControler.runMATSimIterations();
		
		final ChargingTimes chargingTimes = ParametersPSF2.chargingTimes.get(Id.create(255, Person.class));
		Gbl.assertNotNull(chargingTimes);
		LinkedList<ChargeLog> chargingTimesForAgent255 = chargingTimes.getChargingTimes();
		assertEquals(2, chargingTimesForAgent255.size());
		assertEquals(10*3600*1000.0, chargingTimesForAgent255.getLast().getEndSOC());
		
		LinkedList<ChargeLog> chargingTimesForAgent253 = ParametersPSF2.chargingTimes.get(Id.create(253, Person.class)).getChargingTimes();
		assertEquals(2, chargingTimesForAgent255.size());
		assertEquals(10*3600*1000.0, chargingTimesForAgent253.getFirst() .getEndSOC());
		
		ParametersPSF2.setAllowedChargingLocations(null);
	}
	
	public void testEventFileBased(){	
		PSSControler pssControler=new PSSControlerDumbCharging("test/input/scenarios/config-event-file-based.xml", null);
		pssControler.runMATSimIterations();
		
		LinkedList<ChargeLog> chargingTimesForAgent1 = ParametersPSF2.chargingTimes.get(Id.create(1, Person.class)).getChargingTimes();
		assertEquals(2, chargingTimesForAgent1.size());
		
		assertEquals(22846, chargingTimesForAgent1.get(0).getStartChargingTime(),1.0);
		assertEquals(38386, chargingTimesForAgent1.get(1).getStartChargingTime(),1.0);
		
		assertEquals(10*3600*1000.0, chargingTimesForAgent1.getLast().getEndSOC());
	}
	
	private void performSingleAgentRun(){		
		PSSControler pssControler=new PSSControlerDumbCharging("test/input/scenarios/config-event-file-based-oneAgent.xml", null);
		pssControler.runMATSimIterations();
	}
	public void testEventFileBasedOneAgentLocationFilterHome(){
		addLocationFilter("h");
		performSingleAgentRun();
		
		LinkedList<ChargeLog> chargingTimesForAgent1 = ParametersPSF2.chargingTimes.get(Id.create(1, Person.class)).getChargingTimes();
		assertEquals(1, chargingTimesForAgent1.size());
		
		assertEquals(38040, chargingTimesForAgent1.get(0).getStartChargingTime(),1.0);
		
		assertEquals(10*3600*1000.0, chargingTimesForAgent1.getLast().getEndSOC());
	}
	
	
	
	public void testEventFileBasedOneAgentLocationFilterWork(){
		addLocationFilter("w");
		performSingleAgentRun();
		
		LinkedList<ChargeLog> chargingTimesForAgent1 = ParametersPSF2.chargingTimes.get(Id.create(1, Person.class)).getChargingTimes();
		assertEquals(1, chargingTimesForAgent1.size());
		
		assertEquals(22500, chargingTimesForAgent1.get(0).getStartChargingTime(),1.0);
		
		assertEquals(10*3600*1000.0, chargingTimesForAgent1.getLast().getEndSOC());
	}

	private void addLocationFilter(String locationType) {
		LinkedList<String> allowedChargingLocations=new LinkedList<String>();
		
		allowedChargingLocations.add(locationType);
		
		ParametersPSF2.setAllowedChargingLocations(allowedChargingLocations);
	}
}
