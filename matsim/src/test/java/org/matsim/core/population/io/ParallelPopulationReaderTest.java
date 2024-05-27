package org.matsim.core.population.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ParallelPopulationReaderTest {

	@Test
	void testParallelPopulationReaderV4_escalateException() {
		String xml = """
			<?xml version="1.0" ?>
			<!DOCTYPE plans SYSTEM "http://www.matsim.org/files/dtd/plans_v4.dtd">
			<plans>
			<person id="1">
				<plan>
					<act type="h" x="-25000" y="foobar" link="1" end_time="06:00" />
					<leg mode="car">
						<route> </route>
					</leg>
					<act type="h" x="-25000" y="0" link="1" />
				</plan>
			</person>
			</plans>
			""";

		InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		try {
			new ParallelPopulationReaderMatsimV4(scenario).readStream(stream);
			Assertions.fail("Expected exception");
		} catch (Exception expected) {
			expected.printStackTrace();
		}
	}

	@Test
	void testParallelPopulationReaderV6_escalateException() {
		String xml = """
			<?xml version="1.0" encoding="utf-8"?>
			<!DOCTYPE population SYSTEM "http://www.matsim.org/files/dtd/population_v6.dtd">

			<population>
			<person id="1">
				<plan>
					<activity type="h" x="-25000" y="foobar" link="1" end_time="06:00" />
					<leg mode="car">
						<route> </route>
					</leg>
					<activity type="h" x="-25000" y="0" link="1" />
				</plan>
			</person>
			</population>
			""";

		InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		try {
			new ParallelPopulationReaderMatsimV6(null, null, scenario).readStream(stream);
			Assertions.fail("Expected exception");
		} catch (Exception expected) {
			expected.printStackTrace();
		}
	}

}
