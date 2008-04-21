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
package org.matsim.trafficlights.data;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficlights.data.PlanbasedSignalSystemControlInfo;
import org.matsim.trafficlights.data.SignalGroupDefinition;
import org.matsim.trafficlights.data.SignalGroupDefinitionParser;
import org.matsim.trafficlights.data.SignalGroupSettings;
import org.matsim.trafficlights.data.SignalSystemConfiguration;
import org.matsim.trafficlights.data.SignalSystemConfigurationParser;
import org.matsim.trafficlights.data.SignalSystemControlInfo;
import org.matsim.trafficlights.data.SignalSystemPlan;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class SignalSystemConfigurationTest extends MatsimTestCase {

	private static final String TESTXML = "testSignalSystemConfig.xml";

  private static final String TESTGROUPDEFXML  = "testSignalGroupDefinition.xml";


	public void testParser() {
		List<SignalGroupDefinition> signalGroups = new LinkedList<SignalGroupDefinition>();
		SignalGroupDefinitionParser groupParser = new SignalGroupDefinitionParser(signalGroups);
		try {
			groupParser.parse(this.getPackageInputDirectory() + TESTGROUPDEFXML);
			assertEquals(2, signalGroups.size());

			SignalSystemConfigurationParser parser = new SignalSystemConfigurationParser(signalGroups);
			parser.parse(this.getPackageInputDirectory() + TESTXML);
			Map<Id, SignalSystemConfiguration> configs = parser.getSignalSystemConfigurations();
			assertNotNull(configs);
			SignalSystemConfiguration ssc = configs.get(new IdImpl("456"));
			assertNotNull(ssc);
			Set<SignalGroupDefinition> sgDefs = ssc.getSignalGroupDefinitions();
			assertNotNull(sgDefs);
			assertEquals(1, sgDefs.size());
			assertTrue(sgDefs.contains(signalGroups.get(0)));
			SignalSystemControlInfo sysControler = ssc.getSignalSystemControler();
			assertTrue(sysControler instanceof PlanbasedSignalSystemControlInfo);
			PlanbasedSignalSystemControlInfo controler = (PlanbasedSignalSystemControlInfo) sysControler;
			List<SignalSystemPlan> signalPlans = controler.getSignalSystemPlans();
			assertEquals(1, signalPlans.size());
			SignalSystemPlan plan = signalPlans.get(0);
			assertEquals(new IdImpl("7"), plan.getId());
			assertEquals(0.0, plan.getStartTime());
			assertEquals(60.0 * 60.0 * 24.0, plan.getStopTime());
			assertEquals(60, plan.getCirculationTime());
			assertEquals(0, plan.getSyncTime());
			assertEquals(0, plan.getPowerOnTime());
			assertEquals(50, plan.getPowerOffTime());

			SignalGroupSettings settings = plan.getSignalGroupSettings().get(new IdImpl("123"));
			assertNotNull(settings);
			assertEquals(0, settings.getRoughCast());
			assertEquals(45, settings.getDropping());
			assertEquals(2, settings.getInterimTimeRoughcast());
			assertEquals(3, settings.getInterimTimeDropping());

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
