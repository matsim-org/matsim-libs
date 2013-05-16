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

package playground.emissions;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.*;
import playground.vsp.emissions.WarmEmissionAnalysisModule;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventImpl;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonWarmEventHandler;



public class TestWarmEmissionAnalysisModule {
	
	//TODO copy from TestColdEmissionAnalysisModule
	@Test
	public final void testCalculateWarmEmissionsAndThrowEvent(){
		
		//setup? rather without inputfiles
		/*(
			Id coldEmissionEventLinkId,
			Id personId,
			Double startEngineTime,
			Double parkingDuration,
			Double accumulatedDistance,
			String vehicleInformation)*/
		
		//WarmEmissionAnalysisModule weam = new ColdEmissionAnalysisModule(parameterObject, emissionEventsManager, emissionEfficiencyFactor);
		//TODO can i setup a analysismodule without inputfiles? ->ihab/benjamin
		
	}
	
}
	

	

