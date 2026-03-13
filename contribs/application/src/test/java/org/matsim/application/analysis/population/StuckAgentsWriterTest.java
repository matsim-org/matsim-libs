package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

class StuckAgentsWriterTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testStuckAgentsWriter() throws Exception {
		// Create a StuckAgentsWriter
		StuckAgentsWriter writer = new StuckAgentsWriter();

		// Create some mock stuck events
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(writer);
		eventsManager.initProcessing();

		// Fire some PersonStuckEvents
		eventsManager.processEvent(new PersonStuckEvent(
			7200.0,
			Id.createPersonId("person1"),
			Id.createLinkId("link123"),
			"car"
		));

		eventsManager.processEvent(new PersonStuckEvent(
			14400.0,
			Id.createPersonId("person2"),
			Id.createLinkId("link456"),
			"pt"
		));

		eventsManager.processEvent(new PersonStuckEvent(
			18000.0,
			Id.createPersonId("person3"),
			null,  // null link
			"walk"
		));

		eventsManager.finishProcessing();

		// Check that we captured 3 stuck agents
		Assertions.assertEquals(3, writer.getStuckAgentCount());

		// Write the file
		Path outputFile = Path.of(utils.getOutputDirectory(), "stuck_agents.xml");
		writer.writeFile(outputFile.toString());

		// Verify the file exists and has content
		Assertions.assertTrue(Files.exists(outputFile));
		String content = Files.readString(outputFile);

		// Verify XML structure and content
		Assertions.assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		Assertions.assertTrue(content.contains("<stuckAgents"));
		Assertions.assertTrue(content.contains("count=\"3\""));

		// Verify individual stuck agents are present
		Assertions.assertTrue(content.contains("personId=\"person1\""));
		Assertions.assertTrue(content.contains("time=\"02:00:00\""));
		Assertions.assertTrue(content.contains("linkId=\"link123\""));
		Assertions.assertTrue(content.contains("legMode=\"car\""));

		Assertions.assertTrue(content.contains("personId=\"person2\""));
		Assertions.assertTrue(content.contains("time=\"04:00:00\""));
		Assertions.assertTrue(content.contains("linkId=\"link456\""));
		Assertions.assertTrue(content.contains("legMode=\"pt\""));

		Assertions.assertTrue(content.contains("personId=\"person3\""));
		Assertions.assertTrue(content.contains("time=\"05:00:00\""));
		Assertions.assertTrue(content.contains("linkId=\"unknown\""));
		Assertions.assertTrue(content.contains("legMode=\"walk\""));

		Assertions.assertTrue(content.contains("</stuckAgents>"));
	}

	@Test
	void testResetFunctionality() {
		StuckAgentsWriter writer = new StuckAgentsWriter();

		// Add some events
		writer.handleEvent(new PersonStuckEvent(
			7200.0,
			Id.createPersonId("person1"),
			Id.createLinkId("link1"),
			"car"
		));

		Assertions.assertEquals(1, writer.getStuckAgentCount());

		// Reset
		writer.reset();
		Assertions.assertEquals(0, writer.getStuckAgentCount());

		// Add more events
		writer.handleEvent(new PersonStuckEvent(
			14400.0,
			Id.createPersonId("person2"),
			Id.createLinkId("link2"),
			"pt"
		));

		Assertions.assertEquals(1, writer.getStuckAgentCount());
	}

	@Test
	void testCommandLineExecution() throws Exception {
		// Create a simple events file with stuck events
		Path eventsFile = Path.of(utils.getInputDirectory(), "events.xml");
		Files.createDirectories(eventsFile.getParent());

		// Create a minimal events XML file
		String eventsXml = """
			<?xml version="1.0" encoding="UTF-8"?>
			<events version="1.0">
				<event time="7200.0" type="stuckAndAbort" person="person1" link="link123" legMode="car" />
				<event time="14400.0" type="stuckAndAbort" person="person2" link="link456" legMode="pt" />
			</events>
			""";
		Files.writeString(eventsFile, eventsXml);

		// Run the command
		Path outputFile = Path.of(utils.getOutputDirectory(), "stuck_agents.xml");
		new StuckAgentsWriter().execute(
			"--events", eventsFile.toString(),
			"--output", outputFile.toString()
		);

		// Check output file exists
		Assertions.assertTrue(Files.exists(outputFile));

		String content = Files.readString(outputFile);
		Assertions.assertTrue(content.contains("personId=\"person1\""));
		Assertions.assertTrue(content.contains("personId=\"person2\""));
	}
}
