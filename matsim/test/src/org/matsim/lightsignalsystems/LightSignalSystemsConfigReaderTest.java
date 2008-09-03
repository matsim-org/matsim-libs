/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.lightsignalsystems;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author dgrether
 *
 */
public class LightSignalSystemsConfigReaderTest extends MatsimTestCase {

  private static final String TESTXML  = "testLightSignalSystemsConfig.xml";

  private Id id8 = new IdImpl("8");

  private Id id5 = new IdImpl("5");
  
  private Id id23 = new IdImpl("23");
  
  private Id id42 = new IdImpl("42");

  
  
  public void testParser() {
  	List<BasicLightSignalSystemConfiguration> lssConfigs = new ArrayList<BasicLightSignalSystemConfiguration>();
  	MatsimLightSignalSystemConfigurationReader reader = new MatsimLightSignalSystemConfigurationReader(lssConfigs);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);
  	
  	assertEquals(2, lssConfigs.size());
  	//test first
  	BasicLightSignalSystemConfiguration lssConfiguration = lssConfigs.get(0);
  	assertNotNull(lssConfiguration);
  	assertEquals(id23, lssConfiguration.getLightSignalSystemId());
  	BasicPlanBasedLightSignalSystemControlInfo controlInfo = (BasicPlanBasedLightSignalSystemControlInfo) lssConfiguration.getControlInfo();
  	assertNotNull(controlInfo);
  	BasicLightSignalSystemPlan plan =   controlInfo.getPlans().get(id5);
  	assertNotNull(plan);
  	assertEquals(id5, plan.getId());
  	assertEquals(0.0, plan.getStartTime());
  	assertEquals(0.0, plan.getEndTime());
  	assertEquals(1, plan.getGroupConfigs().size());
  	BasicLightSignalGroupConfiguration groupConfig = plan.getGroupConfigs().get(id23);
  	assertNotNull(groupConfig);
  	assertEquals(0.0, groupConfig.getRoughCast());
  	assertEquals(45.0, groupConfig.getDropping());
  	assertEquals(2.0, groupConfig.getInterimTimeRoughcast());
  	assertEquals(3.0, groupConfig.getInterimTimeDropping());
  	
  	//test second
  	lssConfiguration = lssConfigs.get(1);
  	assertNotNull(lssConfiguration);
  	assertEquals(id42, lssConfiguration.getLightSignalSystemId());
  	controlInfo = (BasicPlanBasedLightSignalSystemControlInfo) lssConfiguration.getControlInfo();
  	assertNotNull(controlInfo);
  	plan =   controlInfo.getPlans().get(id8);
  	assertNotNull(plan);
  	assertEquals(id8, plan.getId());
  	assertEquals(0.0, plan.getStartTime());
  	assertEquals(0.0, plan.getEndTime());
  	assertEquals(1, plan.getGroupConfigs().size());
  	groupConfig = plan.getGroupConfigs().get(id23);
  	assertNotNull(groupConfig);
  	assertEquals(0.0, groupConfig.getRoughCast());
  	assertEquals(45.0, groupConfig.getDropping());
  	assertEquals(Double.NaN, groupConfig.getInterimTimeRoughcast());
  	assertEquals(Double.NaN, groupConfig.getInterimTimeDropping());

  	
  	
  }
  
	
}
