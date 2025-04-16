package playground.vsp.changeExpBeta;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;

import java.util.Random;

public class DisturbanceGenerator implements PersonArrivalEventHandler {
	private final Random random = new Random(1);

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String mode = event.getLegMode();



	}
}
