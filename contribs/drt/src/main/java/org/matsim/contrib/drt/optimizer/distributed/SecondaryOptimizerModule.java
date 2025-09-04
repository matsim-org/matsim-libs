package org.matsim.contrib.drt.optimizer.distributed;

import com.google.common.annotations.Beta;
import com.google.inject.Singleton;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.dsim.MessageBroker;

/**
 * Install optimizer that exchanges messages with the head node.
 */
@Beta
public class SecondaryOptimizerModule extends AbstractDvrpModeQSimModule {

	public SecondaryOptimizerModule(String mode) {
		super(mode);
	}

	@Override
	protected void configureQSim() {

		addModalComponent(DrtOptimizer.class, modalProvider(
			getter -> new SecondaryNodeOptimizer(
				getter.get(DrtNodeCommunicator.class),
				getter.get(MessageBroker.class),
				getter.getModal(Fleet.class),
				getter.getModal(ScheduleTimingUpdater.class)
			))
		);
		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));

		bindModal(DrtTaskFactory.class).toInstance(new DrtTaskFactoryImpl());
		bindModal(VrpAgentLogic.DynActionCreator.class).to(modalKey(DrtActionCreator.class));

		bindModal(VrpLegFactory.class).toProvider(modalProvider(getter -> {
			DvrpConfigGroup dvrpCfg = getter.get(DvrpConfigGroup.class);
			MobsimTimer timer = getter.get(MobsimTimer.class);

			return v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.getMobsimMode(), v, OnlineTrackerListener.NO_LISTENER,
				timer);
		})).in(Singleton.class);

		bindModal(ScheduleTimingUpdater.class).toProvider(modalProvider(
			_ ->  {
				throw new UnsupportedOperationException("TODO: Not supported due to API changes.");
			}
		));
	}
}
