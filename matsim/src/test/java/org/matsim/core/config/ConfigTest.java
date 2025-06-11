/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mrieser / senozon
 */
public class ConfigTest {

	@Test
	void testAddModule_beforeLoading() {
		Config config = new Config();
		ConfigTestGroup group = new ConfigTestGroup();

		config.addModule(group);

		Assertions.assertNull(group.getA());
		Assertions.assertNull(group.getB());

		String str = "<?xml version='1.0' encoding='UTF-8' ?>\n" +
				"<!DOCTYPE config SYSTEM \"http://www.matsim.org/files/dtd/config_v1.dtd\">\n" +
				"<config>\n" +
				"	<module name=\"ctg\">\n" +
				"		<param name=\"a\" value=\"aaa\" />\n" +
				"		<param name=\"b\" value=\"bbb\" />\n" +
				"	</module>\n" +
				"</config>";
		new ConfigReader(config).parse(new ByteArrayInputStream(str.getBytes()));

		Assertions.assertEquals("aaa", group.getA());
		Assertions.assertEquals("bbb", group.getB());
	}

	@Test
	void testAddModule_afterLoading() {
		Config config = new Config();
		ConfigTestGroup group = new ConfigTestGroup();

		Assertions.assertNull(group.getA());
		Assertions.assertNull(group.getB());

		String str = "<?xml version='1.0' encoding='UTF-8' ?>\n" +
				"<!DOCTYPE config SYSTEM \"http://www.matsim.org/files/dtd/config_v1.dtd\">\n" +
				"<config>\n" +
				"	<module name=\"ctg\">\n" +
				"		<param name=\"a\" value=\"aaa\" />\n" +
				"		<param name=\"b\" value=\"bbb\" />\n" +
				"	</module>\n" +
				"</config>";
		new ConfigReader(config).parse(new ByteArrayInputStream(str.getBytes()));

		Assertions.assertEquals("aaa", config.getModule("ctg").getParams().get("a"));
		Assertions.assertEquals("bbb", config.getModule("ctg").getParams().get("b"));
		Assertions.assertNull(group.getA());
		Assertions.assertNull(group.getB());

		config.addModule(group);

		Assertions.assertEquals("aaa", group.getA());
		Assertions.assertEquals("bbb", group.getB());
	}

	private static class ConfigTestGroup extends ConfigGroup {

		public static final String GROUP_NAME = "ctg";

		private static final String PARAM_A = "a";
		private static final String PARAM_B = "b";

		private String a = null;
		private String b = null;

		public ConfigTestGroup() {
			super(GROUP_NAME);
		}

		@Override
		public void addParam(final String paramName, final String value) {
			if (PARAM_A.equals(paramName)) {
				setA(value);
			} else if (PARAM_B.equals(paramName)) {
				setB(value);
			}
		}

		/*package*/ void setA(final String a) {
			this.a = a;
		}

		public String getA() {
			return a;
		}

		/*package*/ void setB(final String b) {
			this.b = b;
		}

		public String getB() {
			return b;
		}

	}

}
