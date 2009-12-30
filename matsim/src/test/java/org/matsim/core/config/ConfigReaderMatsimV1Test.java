/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigTest.java
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

package org.matsim.core.config;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.AttributesBuilder;
import org.xml.sax.Attributes;

public class ConfigReaderMatsimV1Test extends MatsimTestCase {

	/**
	 * Tests that values with the String "null" (case-insensitive) are
	 * <b>not</b> passed to a config-module, but are interpreted as
	 * a default value.
	 *
	 * @author mrieser
	 */
	public void testNullParam() {
		Config config = new Config();
		TestModule testModule = new TestModule("test");
		config.addModule("test", testModule);

		ConfigReaderMatsimV1 reader = new ConfigReaderMatsimV1(config);
		Attributes atts = new AttributesBuilder().add("name", "test").get();
		reader.startTag("module", atts, null);
		atts = new AttributesBuilder().add("name", "param1").add("value", "value1").get();
		reader.startTag("param", atts, null);
		atts = new AttributesBuilder().add("name", "param2").add("value", "null").get();
		reader.startTag("param", atts, null);
		atts = new AttributesBuilder().add("name", "param3").add("value", "NUll").get(); // test for case-insensitive null
		reader.startTag("param", atts, null);
		atts = new AttributesBuilder().add("name", "param4").add("value", "[null]").get(); // test for null-substring
		reader.startTag("param", atts, null);

		assertTrue(testModule.param1set);
		assertFalse(testModule.param2set);
		assertFalse(testModule.param3set);
		assertTrue(testModule.param4set);
	}

	/**
	 * A Test Config-Group used by {@link ConfigReaderMatsimV1Test#testNullParam()}.
	 *
	 * @author mrieser
	 */
	/*default*/ static class TestModule extends Module {

		private static final long serialVersionUID = 1L;

		/*default*/ boolean param1set = false;
		/*default*/ boolean param2set = false;
		/*default*/ boolean param3set = false;
		/*default*/ boolean param4set = false;

		public TestModule(final String name) {
			super(name);
		}

		@Override
		public void addParam(final String paramname, final String value) {
			if ("param1".equals(paramname)) this.param1set = true;
			if ("param2".equals(paramname)) this.param2set = true;
			if ("param3".equals(paramname)) this.param3set = true;
			if ("param4".equals(paramname)) this.param4set = true;
		}

	}

}
