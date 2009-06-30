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

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.signalsystems.config.BasicAdaptivePlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;
import org.matsim.testcases.MatsimTestCase;

/**
 * Test for the readers and writers of the (light)signalSystemConfigurations_v1.*.xsd
 * file formats.
 * @author dgrether
 */
public class SignalSystemsConfigReaderTest extends MatsimTestCase {

  private static final String TESTXML  = "testSignalSystemConfigurations_v1.1.xml";

  private Id id8 = new IdImpl("8");

  private Id id5 = new IdImpl("5");
  
  private Id id23 = new IdImpl("23");
  private Id id24 = new IdImpl("24");
  
  private Id id42 = new IdImpl("42");
  private Id id43 = new IdImpl("43");

  
  
  public void testParser() {
  	BasicSignalSystemConfigurations lssConfigs = new BasicSignalSystemConfigurationsImpl();
  	MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(lssConfigs);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);
  }
  
  public void testWriter() {
  	String testoutput = this.getOutputDirectory()  + "testLssConfigOutput.xml";
  	//read the test file
  	BasicSignalSystemConfigurations lssConfigs = new BasicSignalSystemConfigurationsImpl();
  	MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(lssConfigs);
  	reader.readFile(this.getPackageInputDirectory() + TESTXML);

  	//write the test file
  	MatsimSignalSystemConfigurationsWriter writer = new MatsimSignalSystemConfigurationsWriter(lssConfigs);
  	writer.writeFile(testoutput);
  	
  	lssConfigs = new BasicSignalSystemConfigurationsImpl();
  	reader = new MatsimSignalSystemConfigurationsReader(lssConfigs);
  	reader.readFile(testoutput);
  	checkContent(lssConfigs);
  }

	private void checkContent(BasicSignalSystemConfigurations lssConfigs) {
		assertEquals(4, lssConfigs.getSignalSystemConfigurations().size());
		//test first
		BasicSignalSystemConfiguration lssConfiguration = lssConfigs.getSignalSystemConfigurations().get(id23);
		assertNotNull(lssConfiguration);
		assertEquals(id23, lssConfiguration.getSignalSystemId());
		BasicPlanBasedSignalSystemControlInfo controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfiguration.getControlInfo();
		assertNotNull(controlInfo);
		BasicSignalSystemPlan plan =   controlInfo.getPlans().get(id5);
		assertNotNull(plan);
		assertEquals(id5, plan.getId());
		assertEquals(0.0, plan.getStartTime(), EPSILON);
		assertEquals(0.0, plan.getEndTime(), EPSILON);
		assertEquals(Integer.valueOf(40), plan.getCycleTime());
		assertEquals(Integer.valueOf(3), plan.getSynchronizationOffset());
		
		assertEquals(1, plan.getGroupConfigs().size());
		BasicSignalGroupSettings groupConfig = plan.getGroupConfigs().get(id23);
		assertNotNull(groupConfig);
		assertEquals(0.0, groupConfig.getRoughCast(), EPSILON);
		assertEquals(45.0, groupConfig.getDropping(), EPSILON);
		assertEquals(Integer.valueOf(2), groupConfig.getInterimGreenTimeRoughcast());
		assertEquals(Integer.valueOf(3), groupConfig.getInterGreenTimeDropping());
		
		//test second
		lssConfiguration = lssConfigs.getSignalSystemConfigurations().get(id42);
		assertNotNull(lssConfiguration);
		assertEquals(id42, lssConfiguration.getSignalSystemId());
		controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfiguration.getControlInfo();
		assertNotNull(controlInfo);
		plan =   controlInfo.getPlans().get(id8);
		assertNotNull(plan);
		assertEquals(id8, plan.getId());
		assertEquals(0.0, plan.getStartTime(), EPSILON);
		assertEquals(0.0, plan.getEndTime(), EPSILON);
		assertEquals(Integer.valueOf(60), plan.getCycleTime());
		assertNull(plan.getSynchronizationOffset());
		assertEquals(1, plan.getGroupConfigs().size());
		groupConfig = plan.getGroupConfigs().get(id23);
		assertNotNull(groupConfig);
		assertEquals(0.0, groupConfig.getRoughCast(), EPSILON);
		assertEquals(45.0, groupConfig.getDropping(), EPSILON);
		assertNull(groupConfig.getInterimGreenTimeRoughcast());
		assertNull(groupConfig.getInterGreenTimeDropping());
		
		//test 3rd
		lssConfiguration = lssConfigs.getSignalSystemConfigurations().get(id43);
		assertNotNull(lssConfiguration);
		assertEquals(id43, lssConfiguration.getSignalSystemId());
		BasicAdaptiveSignalSystemControlInfo adaptiveControlInfo = (BasicAdaptiveSignalSystemControlInfo) lssConfiguration.getControlInfo();
		assertNotNull(adaptiveControlInfo);
		assertEquals("org.matism.nonexistingpackage.Nonexistingcontroler", adaptiveControlInfo.getAdaptiveControlerClass());
		assertEquals(2, adaptiveControlInfo.getSignalGroupIds().size());
		assertEquals(id23, adaptiveControlInfo.getSignalGroupIds().get(0));
		assertEquals(id42, adaptiveControlInfo.getSignalGroupIds().get(1));
		
		
		// test 4th
		lssConfiguration = lssConfigs.getSignalSystemConfigurations().get(id24);
		assertNotNull(lssConfiguration);
		assertEquals(id24, lssConfiguration.getSignalSystemId());
		BasicAdaptivePlanBasedSignalSystemControlInfo adaptivePbControlInfo = (BasicAdaptivePlanBasedSignalSystemControlInfo) lssConfiguration.getControlInfo();
		assertNotNull(adaptivePbControlInfo);
		assertEquals("org.matism.nonexistingpackage.Nonexistingcontroler", adaptiveControlInfo.getAdaptiveControlerClass());
		assertEquals(2, adaptivePbControlInfo.getSignalGroupIds().size());
		assertEquals(id23, adaptivePbControlInfo.getSignalGroupIds().get(0));
		assertEquals(id42, adaptivePbControlInfo.getSignalGroupIds().get(1));
		
		plan =   adaptivePbControlInfo.getPlans().get(id5);
		assertNotNull(plan);
		assertEquals(id5, plan.getId());
		assertEquals(0.0, plan.getStartTime(), EPSILON);
		assertEquals(0.0, plan.getEndTime(), EPSILON);
		assertEquals(Integer.valueOf(40), plan.getCycleTime());
		assertEquals(Integer.valueOf(3), plan.getSynchronizationOffset());
		
		assertEquals(1, plan.getGroupConfigs().size());
		groupConfig = plan.getGroupConfigs().get(id23);
		assertNotNull(groupConfig);
		assertEquals(0, groupConfig.getRoughCast().intValue());
		assertEquals(45, groupConfig.getDropping().intValue());
		assertEquals(Integer.valueOf(2), groupConfig.getInterimGreenTimeRoughcast());
		assertEquals(Integer.valueOf(3), groupConfig.getInterGreenTimeDropping());
	}  
	
}
