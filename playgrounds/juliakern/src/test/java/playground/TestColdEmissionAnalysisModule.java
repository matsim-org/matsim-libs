/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestEmission.java                                                       *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.*;
import playground.vsp.emissions.ColdEmissionAnalysisModule;
import playground.vsp.emissions.events.ColdEmissionEvent;
import playground.vsp.emissions.events.ColdEmissionEventImpl;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler;


public class TestColdEmissionAnalysisModule {

	@Test @Ignore
	public void calculateColdEmissionsAndThrowEventTest() {
		
		//TODO kann ich das ohne Inputdateien initialisieren?
		ColdEmissionAnalysisModule ceam = new ColdEmissionAnalysisModule(null, null, null);
		//warum ist hier die Parkzeti als Double in anderen Methoden aber als int?
		ceam.calculateColdEmissionsAndThrowEvent(new IdImpl("coldEmissionEventLinkId"), 
				new IdImpl("personId"),
				10.0,
				2.0,
				50.0,
				"vehicleInformation") ;
		Assert.assertEquals("something", true, true);
	}
	
	@Test @Ignore //is private.... test it anyway?
	public void calculateColdEmissionsTest() {
		Assert.assertEquals("something", true, true);
	}
	
	@Test @Ignore //is private.... test it anyway?
	public void convertString2TupleTest(){
		Assert.assertEquals("something", true, true);
	}

	//is private.... test it anyway?
	//TODO sollte rescale Emissions auch getestet werden? 
}
