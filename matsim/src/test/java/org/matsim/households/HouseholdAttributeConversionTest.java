package org.matsim.households;



import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Objects;
import java.util.function.Consumer;

public class HouseholdAttributeConversionTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDefaults() {
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

		Assert.assertEquals(
				"unexpected read attribute",
				attribute,
				readAttribute);
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
