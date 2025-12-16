package org.matsim.core.config.consistency;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UnmaterializedConfigGroupCheckerTest {

	@Test
	void testUnmaterializedConfigGroup() {
		String xml = """
				<?xml version="1.0" ?>
				<!DOCTYPE config SYSTEM "http://www.matsim.org/files/dtd/config_v2.dtd">
				<config>
					<module name="something">
						<param name="endTime" value="30:00:00"/>
					</module>
				</config>
				""";
		ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		Config config = ConfigUtils.createConfig();
		ConfigReader reader = new ConfigReader(config);
		reader.parse(bais);

		assertNotNull(config.getModules().get("something"));

		Assertions.assertThrows(RuntimeException.class, () -> new UnmaterializedConfigGroupChecker().checkConsistency(config));
	}

	@Test
	void testUnmaterializedJDEQSim() {
		String xml = """
				<?xml version="1.0" ?>
				<!DOCTYPE config SYSTEM "http://www.matsim.org/files/dtd/config_v2.dtd">
				<config>
					<module name="jdeqsim">
						<param name="endTime" value="30:00:00"/>
					</module>
				</config>
				""";
		ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

		Config config = ConfigUtils.createConfig();
		ConfigReader reader = new ConfigReader(config);
		reader.parse(bais);

		assertNotNull(config.getModules().get("jdeqsim"));

		new UnmaterializedConfigGroupChecker().checkConsistency(config);
		// no runtime exception, but hopefully a log warning…
	}

}
