package playground.gregor.sim2d_v2.calibration.simulation.floor;

import java.util.Queue;

import org.matsim.core.api.experimental.events.Event;

import playground.gregor.sim2d_v2.simulation.Sim2D;
import playground.gregor.sim2d_v2.simulation.floor.Floor;

public class PhantomFloor implements Floor {


	private final Queue<Event> phantomPopulation;
	private final Sim2D sim;

	public PhantomFloor(Queue<Event> phantomPopulation, Sim2D sim) {
		this.sim = sim;
		this.phantomPopulation = phantomPopulation;
	}

	@Override
	public void move(double time) {
		while (this.phantomPopulation.peek().getTime() < time) {
			this.sim.getEventsManager().processEvent(this.phantomPopulation.poll());
		}
	}
}
