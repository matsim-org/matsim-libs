package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

class SlaveCommandLineParserTest {

	private final SlaveCommandLineParser parser = new SlaveCommandLineParser();

	@Test
	void parsesAllNamedOptions() throws ParseException {
		SlaveLaunchArguments arguments = parser.parse(new String[]{
				"--config", "named.xml", "--host", "worker.example", "--port", "23456", "--threads", "8"});

		assertEquals("named.xml", arguments.configFile());
		assertEquals("worker.example", arguments.hostname());
		assertEquals("23456", arguments.port());
		assertEquals("8", arguments.threads());
	}

	@Test
	void usesFirstPositionalArgumentAsConfigFile() throws ParseException {
		SlaveLaunchArguments arguments = parser.parse(new String[]{"positional.xml", "ignored.xml"});

		assertEquals("positional.xml", arguments.configFile());
	}

	@Test
	void namedConfigTakesPrecedenceOverPositionalArgument() throws ParseException {
		SlaveLaunchArguments arguments = parser.parse(new String[]{"positional.xml", "-c", "named.xml"});

		assertEquals("named.xml", arguments.configFile());
	}

	@Test
	void representsMissingOptionsAsNull() throws ParseException {
		SlaveLaunchArguments arguments = parser.parse(new String[0]);

		assertNull(arguments.configFile());
		assertNull(arguments.hostname());
		assertNull(arguments.port());
		assertNull(arguments.threads());
	}
}
