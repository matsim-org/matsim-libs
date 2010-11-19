package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;

public class MobsimTask implements ScenarioSinkSourceEventSource {

	private ScenarioSink sink;
	
	private MobsimFactory mobsimFactory;
	
	private EventsManager eventsManager;
	
	public MobsimTask(MobsimFactory mobsimFactory) {
		this.mobsimFactory = mobsimFactory;
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	@Override
	public void initialize(Scenario scenario) {
		this.sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		Simulation mobsim = mobsimFactory.createMobsim(scenario, eventsManager);
		mobsim.run();
		sink.process(scenario);
	}

	public void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

}
