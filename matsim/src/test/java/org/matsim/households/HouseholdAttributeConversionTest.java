
/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdAttributeConversionTest.java
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

 package org.matsim.households;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Objects;
import java.util.function.Consumer;

	public class HouseholdAttributeConversionTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testDefaults() {
		final String path = utils.getOutputDirectory()+"/households.xml";

		testWriteAndReread(w -> w.writeFile(path), w -> w.readFile(path));
	}

	public void testWriteAndReread(
			final Consumer<HouseholdsWriterV10> writeMethod,
			final Consumer<HouseholdsReaderV10> readMethod) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Households households = scenario.getHouseholds();
		final HouseholdsFactory factory = households.getFactory();

		final Id<Household> id = Id.create("id", Household.class);
		final Household household = factory.createHousehold(id);
		households.getHouseholds().put(id, household);

		final CustomClass attribute = new CustomClass("attribute");
		household.getAttributes().putAttribute("attribute", attribute);

		final HouseholdsWriterV10 writer = new HouseholdsWriterV10(scenario.getHouseholds());
		writer.putAttributeConverter(CustomClass.class, new CustomClassConverter());
		writeMethod.accept(writer);

		final Scenario readScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final HouseholdsReaderV10 reader = new HouseholdsReaderV10(readScenario.getHouseholds());
		reader.putAttributeConverter(CustomClass.class, new CustomClassConverter());
		readMethod.accept(reader);

		final Households readHouseholds = readScenario.getHouseholds();
		final Household readHousehold = readHouseholds.getHouseholds().get(id);
		final Object readAttribute = readHousehold.getAttributes().getAttribute("attribute");

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
