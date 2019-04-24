package org.matsim.facilities;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Objects;
import java.util.function.Consumer;

public class FacilitiesAttributeConvertionTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDefaults() {
		final String path = utils.getOutputDirectory()+"/facilities.xml";

		testWriteAndReread(w -> w.write(path), w -> w.readFile(path));
	}

	@Test
	public void testV1() {
		final String path = utils.getOutputDirectory()+"/facilities.xml";

		testWriteAndReread(w -> w.writeV1(path), w -> w.readFile(path));
	}

	@Test
	public void testDefaultsStream() {
		final String path = utils.getOutputDirectory()+"/facilities.xml";

		testWriteAndReread(w -> w.write(IOUtils.getOutputStream(path)), w -> w.readFile(path));
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
