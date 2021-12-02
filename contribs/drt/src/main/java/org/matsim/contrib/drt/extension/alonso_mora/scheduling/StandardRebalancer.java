package org.matsim.contrib.drt.extension.alonso_mora.scheduling;

import java.util.List;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

/**
 * This is mostly copy & paste from DefaultDrtOptimizer to replicate Drt
 * rebalancing with the Alonso Mora optimizer.
 * 
 * @author shoerl
 *
 */
public class StandardRebalancer implements MobsimBeforeSimStepListener {
	private final RebalancingStrategy rebalancingStrategy;
	private final MobsimTimer mobsimTimer;
	private final ScheduleInquiry scheduleInquiry;
	private final Fleet fleet;
	private final EmptyVehicleRelocator relocator;
	private final Double rebalancingInterval;

	public StandardRebalancer(RebalancingStrategy rebalancingStrategy, MobsimTimer mobsimTimer,
			ScheduleInquiry scheduleInquiry, Fleet fleet, EmptyVehicleRelocator relocator, Double rebalancingInterval) {
		this.rebalancingStrategy = rebalancingStrategy;
		this.mobsimTimer = mobsimTimer;
		this.scheduleInquiry = scheduleInquiry;
		this.fleet = fleet;
		this.relocator = relocator;
		this.rebalancingInterval = rebalancingInterval;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (rebalancingInterval != null && e.getSimulationTime() % rebalancingInterval == 0) {
			rebalanceFleet();
		}
	}

	private void rebalanceFleet() {
		// right now we relocate only idle vehicles (vehicles that are being relocated
		// cannot be relocated)
		Stream<? extends DvrpVehicle> rebalancableVehicles = fleet.getVehicles().values().stream()
				.filter(scheduleInquiry::isIdle);
		List<Relocation> relocations = rebalancingStrategy.calcRelocations(rebalancableVehicles,
				mobsimTimer.getTimeOfDay());

		if (!relocations.isEmpty()) {
			for (Relocation r : relocations) {
				Link currentLink = ((DrtStayTask) r.vehicle.getSchedule().getCurrentTask()).getLink();
				if (currentLink != r.link) {
					relocator.relocateVehicle(r.vehicle, r.link);
				}
			}
		}
	}

	public boolean isActive() {
		return !(rebalancingStrategy instanceof NoRebalancingStrategy);
	}
}
