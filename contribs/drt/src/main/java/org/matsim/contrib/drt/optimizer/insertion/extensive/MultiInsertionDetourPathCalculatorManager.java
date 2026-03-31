package org.matsim.contrib.drt.optimizer.insertion.extensive;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayList;
import java.util.List;

public class MultiInsertionDetourPathCalculatorManager implements MobsimBeforeCleanupListener {

	private final Network network;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final DrtConfigGroup drtCfg;
	private final List<MultiInsertionDetourPathCalculator> multiInsertionDetourPathCalculatorList;

	MultiInsertionDetourPathCalculatorManager(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
											  DrtConfigGroup drtCfg)
	{
		this.network = network;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.drtCfg = drtCfg;
		this.multiInsertionDetourPathCalculatorList = new ArrayList<>();
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		multiInsertionDetourPathCalculatorList.forEach(i -> i.notifyMobsimBeforeCleanup(e));
	}



	MultiInsertionDetourPathCalculator create()
	{
		MultiInsertionDetourPathCalculator instance =  new MultiInsertionDetourPathCalculator(network, travelTime, travelDisutility, drtCfg);
		this.multiInsertionDetourPathCalculatorList.add(instance);
		return instance;
	}
}
