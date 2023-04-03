package org.matsim.simwrapper.analysis;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(name = "todo", description = "todo")
@CommandSpec(requireEvents = true, produces = {"table1.md", "output2.xml.gz", "...."})
public class StuckAgentAnalysis implements MATSimAppCommand, PersonStuckEventHandler {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(StuckAgentAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(StuckAgentAnalysis.class);

	public static void main(String[] args) {
		new StuckAgentAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this);

		manager.initProcessing();

		EventsUtils.readEvents(manager, input.getEventsPath());

		manager.finishProcessing();

		return 0;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		System.out.println(event);

	}
}
