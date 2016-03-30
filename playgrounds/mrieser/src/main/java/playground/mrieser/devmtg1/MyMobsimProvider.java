package playground.mrieser.devmtg1;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;

public class MyMobsimProvider implements Provider<Mobsim> {

	@Inject
	private Scenario scenario;
	@Inject
	private EventsManager events;
	@Inject
	private MyDummyClass mine;
	
	@Override
	public Mobsim get() {
		System.out.println(mine.toString());
		return new QSim(scenario, events);
	}

}
