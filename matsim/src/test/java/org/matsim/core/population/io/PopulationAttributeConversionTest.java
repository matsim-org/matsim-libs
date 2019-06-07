package org.matsim.core.population.io;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.util.Objects;
import java.util.function.Consumer;

public class PopulationAttributeConversionTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDefaults() {
		final String path = utils.getOutputDirectory()+"/plans.xml";

		testWriteAndReread(w -> w.write(path), w -> w.readFile(path));
	}

	@Test
	public void testDefaultsStream() {
		final String path = utils.getOutputDirectory()+"/plans.xml";

		testWriteAndReread(w -> w.write(IOUtils.getOutputStream(path)), w -> w.readFile(path));
	}

	@Test
	public void testV6() {
		final String path = utils.getOutputDirectory()+"/plans.xml";

		testWriteAndReread(w -> w.writeV6(path), w -> w.readFile(path));
	}

	@Test
	public void testV7() {
		final String path = utils.getOutputDirectory()+"/plans.xml";

		testWriteAndReread(w -> w.writeV6(IOUtils.getOutputStream(path)), w -> w.readFile(path));
	}

	public void testWriteAndReread(
			final Consumer<PopulationWriter> writeMethod,
			final Consumer<PopulationReader> readMethod) {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Population population = scenario.getPopulation();
		final PopulationFactory populationFactory = population.getFactory();

		final Id<Person> personId = Id.createPersonId("Gaston");
		final Person person = populationFactory.createPerson(personId);
		final CustomClass attribute = new CustomClass("homme à tout faire");
		person.getAttributes().putAttribute("job", attribute);
		population.addPerson(person);

		final PopulationWriter writer = new PopulationWriter(population);
		writer.putAttributeConverter(CustomClass.class, new CustomClassConverter());
		writeMethod.accept(writer);

		final Scenario readScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final PopulationReader reader = new PopulationReader(readScenario);
		reader.putAttributeConverter(CustomClass.class, new CustomClassConverter());
		readMethod.accept(reader);

		final Person readPerson = readScenario.getPopulation().getPersons().get(personId);
		final Object readAttribute = readPerson.getAttributes().getAttribute("job");

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
