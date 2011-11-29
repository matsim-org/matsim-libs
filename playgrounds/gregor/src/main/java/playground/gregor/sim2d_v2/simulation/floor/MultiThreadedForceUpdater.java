package playground.gregor.sim2d_v2.simulation.floor;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.ForceModule;

public class MultiThreadedForceUpdater implements Runnable{
	private final List<DynamicForceModule> dynamicForceModules;
	private final List<ForceModule> forceModules;
	private final BlockingQueue<Agent2D> queue;
	private final double time;

	public MultiThreadedForceUpdater(BlockingQueue<Agent2D> queue, List<ForceModule> forceModules, List<DynamicForceModule> dynamicForceModules, double time) {
		this.queue = queue;
		this.forceModules = forceModules;
		this.dynamicForceModules = dynamicForceModules;
		this.time = time;
	}

	@Override
	public void run(){


		while (true) {
			Agent2D agent = this.queue.poll();
			if (agent == null) {
				return;
			}
			for (ForceModule m : this.dynamicForceModules) {
				m.run(agent,this.time);
			}
			for (ForceModule m : this.forceModules) {
				m.run(agent,this.time);
			}
		}
	}

}
