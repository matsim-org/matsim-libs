/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.signalsystems;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.signalsystemsconfig.BasicSignalGroupConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.signalsystems.MatsimLightSignalSystemConfigurationReader;
import org.matsim.signalsystems.MatsimLightSignalSystemConfigurationWriter;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class LightSignalSystemsConfigReaderTest extends MatsimTestCase {

  private static final String TESTXML  = "testLightSignalSystemsConfig.xml";

  private Id id8 = new IdImpl("8");

  private Id id5 = new IdImpl("5");
  
  private Id id23 = new IdImpl("23");
  
  private Id id42 = new IdImpl("42");

  
  
  public void estParser() {
  	List<BasicSignalSystemConfiguration> lssConfigs = new ArrayList<BasicSignalSystemConfiguration>();
  	MatsimLightSignalSystemConfigurationReader reader = new MatsimLightSignalSystemConfigurationReader(lssConfigs);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);
  }
  
  public void testWriter() {
  	String testoutput = this.getOutputDirectory()  + "testLssConfigOutput.xml";
  	//read the test file
  	List<BasicSignalSystemConfiguration> lssConfigs = new ArrayList<BasicSignalSystemConfiguration>();
  	MatsimLightSignalSystemConfigurationReader reader = new MatsimLightSignalSystemConfigurationReader(lssConfigs);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);

  	//write the test file
  	MatsimLightSignalSystemConfigurationWriter writer = new MatsimLightSignalSystemConfigurationWriter(lssConfigs);
  	writer.writeFile(testoutput);
  	
  	lssConfigs = new ArrayList<BasicSignalSystemConfiguration>();
  	reader = new MatsimLightSignalSystemConfigurationReader(lssConfigs);
  	reader.readFile(testoutput);
  	checkContent(lssConfigs);
  }

	private void checkContent(List<BasicSignalSystemConfiguration> lssConfigs) {
		assertEquals(2, lssConfigs.size());
		//test first
		BasicSignalSystemConfiguration lssConfiguration = lssConfigs.get(0);
		assertNotNull(lssConfiguration);
		assertEquals(id23, lssConfiguration.getLightSignalSystemId());
		BasicPlanBasedSignalSystemControlInfo controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfiguration.getControlInfo();
		assertNotNull(controlInfo);
		BasicSignalSystemPlan plan =   controlInfo.getPlans().get(id5);
		assertNotNull(plan);
		assertEquals(id5, plan.getId());
		assertEquals(0.0, plan.getStartTime(), EPSILON);
		assertEquals(0.0, plan.getEndTime(), EPSILON);
		assertEquals(40.0, plan.getCirculationTime());
		assertEquals(3.0, plan.getSyncronizationOffset());
		
		assertEquals(1, plan.getGroupConfigs().size());
		BasicSignalGroupConfiguration groupConfig = plan.getGroupConfigs().get(id23);
		assertNotNull(groupConfig);
		assertEquals(0.0, groupConfig.getRoughCast(), EPSILON);
		assertEquals(45.0, groupConfig.getDropping(), EPSILON);
		assertEquals(2.0, groupConfig.getInterimTimeRoughcast());
		assertEquals(3.0, groupConfig.getInterimTimeDropping());
		
		//test second
		lssConfiguration = lssConfigs.get(1);
		assertNotNull(lssConfiguration);
		assertEquals(id42, lssConfiguration.getLightSignalSystemId());
		controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfiguration.getControlInfo();
		assertNotNull(controlInfo);
		plan =   controlInfo.getPlans().get(id8);
		assertNotNull(plan);
		assertEquals(id8, plan.getId());
		assertEquals(0.0, plan.getStartTime(), EPSILON);
		assertEquals(0.0, plan.getEndTime(), EPSILON);
		assertNull(plan.getCirculationTime());
		assertNull(plan.getSyncronizationOffset());
		assertEquals(1, plan.getGroupConfigs().size());
		groupConfig = plan.getGroupConfigs().get(id23);
		assertNotNull(groupConfig);
		assertEquals(0.0, groupConfig.getRoughCast(), EPSILON);
		assertEquals(45.0, groupConfig.getDropping(), EPSILON);
		assertNull(groupConfig.getInterimTimeRoughcast());
		assertNull(groupConfig.getInterimTimeDropping());
	}  
	
}
