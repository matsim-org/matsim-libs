
/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesAttributeConvertionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.facilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Objects;
import java.util.function.Consumer;

	public class FacilitiesAttributeConvertionTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testDefaults() {
		final String path = utils.getOutputDirectory()+"/facilities.xml";

		testWriteAndReread(w -> w.write(path), w -> w.readFile(path));
	}

	 @Test
	 void testV1() {
		final String path = utils.getOutputDirectory()+"/facilities.xml";

		testWriteAndReread(w -> w.writeV1(path), w -> w.readFile(path));
	}

	 @Test
	 void testDefaultsStream() {
		final String path = utils.getOutputDirectory()+"/facilities.xml";

		testWriteAndReread(w -> w.write(IOUtils.getOutputStream(IOUtils.getFileUrl(path), false)), w -> w.readFile(path));
	}

	public void testWriteAndReread(
			final Consumer<FacilitiesWriter> writeMethod,
			final Consumer<MatsimFacilitiesReader> readMethod) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final ActivityFacilities facilities = scenario.getActivityFacilities();

		final CustomClass attribute = new CustomClass("attribute");
		facilities.getAttributes().putAttribute("attribute", attribute);

		final FacilitiesWriter writer = new FacilitiesWriter(scenario.getActivityFacilities());
		writer.putAttributeConverter(CustomClass.class, new CustomClassConverter());
		writeMethod.accept(writer);

		final Scenario readScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final MatsimFacilitiesReader reader = new MatsimFacilitiesReader(readScenario);
		reader.putAttributeConverter(CustomClass.class, new CustomClassConverter());
		readMethod.accept(reader);

		final ActivityFacilities readFacilities = readScenario.getActivityFacilities();
		final Object readAttribute = readFacilities.getAttributes().getAttribute("attribute");

		Assertions.assertEquals(
				attribute,
				readAttribute,
				"unexpected read attribute");
	}

	private static class CustomClass {
		private final String value;

		private CustomClass(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CustomClass that = (CustomClass) o;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}
	}

	private static class CustomClassConverter implements AttributeConverter<CustomClass> {

		@Override
		public CustomClass convert(String value) {
			return new CustomClass(value);
		}

		@Override
		public String convertToString(Object o) {
			return ((CustomClass) o).value;
		}
	}
}
