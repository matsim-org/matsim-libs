package playground.vsp.changeExpBeta;

import com.google.inject.Inject;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.Random;

import static playground.vsp.changeExpBeta.RunTestingScenario.modeA;
import static playground.vsp.changeExpBeta.RunTestingScenario.modeB;

public class DisturbanceGenerator implements PersonArrivalEventHandler {
	private final double stdModeA;
	private final double stdModeB;
	private final Random random = new Random(1);

	@Inject
	EventsManager events;

	public DisturbanceGenerator(double stdModeA, double stdModeB) {
		this.stdModeA = stdModeA;
		this.stdModeB = stdModeB;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String mode = event.getLegMode();
		switch (mode) {
			case modeA:
				double extraScoreA = random.nextGaussian() * stdModeA;
				events.processEvent(new PersonScoreEvent(event.getTime(), event.getPersonId(), extraScoreA, "disturbance in score for mode A"));
				break;
			case modeB:
				double extraScoreB = random.nextGaussian() * stdModeB;
				events.processEvent(new PersonScoreEvent(event.getTime(), event.getPersonId(), extraScoreB, "disturbance in score for mode B"));
				break;
		}
	}
}
