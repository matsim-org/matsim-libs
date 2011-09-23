package playground.gregor.sim2d_v2.simulation.floor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MultiThreadedForceUpdater implements Runnable{
	private final List<DynamicForceModule> dynamicForceModules;
	private final List<ForceModule> forceModules;
	private final BlockingQueue<Agent2D> queue;

	public MultiThreadedForceUpdater(BlockingQueue<Agent2D> queue, List<ForceModule> forceModules, List<DynamicForceModule> dynamicForceModules) {
		this.queue = queue;
		this.forceModules = forceModules;
		this.dynamicForceModules = dynamicForceModules;
	}

	@Override
	public void run(){

		while (true) {
			Agent2D agent = this.queue.poll();
			if (agent == null) {
				return;
			}
			for (ForceModule m : this.dynamicForceModules) {
				m.run(agent);
			}
			for (ForceModule m : this.forceModules) {
				m.run(agent);
			}
		}
	}

}
