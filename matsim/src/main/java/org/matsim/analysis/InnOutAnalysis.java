package org.matsim.analysis;

import org.matsim.core.events.EventsUtils;

public class InnOutAnalysis {

	public static void main(String[] args) {

		var handler = new SimplePersonEventHandler();
		var manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);

		EventsUtils.readEvents(manager, "C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\pt-tutorial\\output\\pt-tutorial\\output_events.xml.gz");

	}
}
