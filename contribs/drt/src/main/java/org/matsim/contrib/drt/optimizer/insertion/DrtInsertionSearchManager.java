package org.matsim.contrib.drt.optimizer.insertion;

import com.google.inject.Provider;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import java.util.ArrayList;
import java.util.List;

public class DrtInsertionSearchManager implements MobsimBeforeCleanupListener {
	private final List<DrtInsertionSearch> drtInsertionSearchList = new ArrayList<>();
	private final Provider<DrtInsertionSearch> drtInsertionSearchProvider;

	public DrtInsertionSearchManager(Provider<DrtInsertionSearch> drtInsertionSearchProvider)
	{
		this.drtInsertionSearchProvider = drtInsertionSearchProvider;
	}

	public DrtInsertionSearch create() {
		DrtInsertionSearch instance = drtInsertionSearchProvider.get();
		this.drtInsertionSearchList.add(instance);
		return instance;
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		drtInsertionSearchList.stream().filter(c -> c instanceof MobsimBeforeCleanupListener )
			.forEach(c -> ((MobsimBeforeCleanupListener) c).notifyMobsimBeforeCleanup(e)  );
	}
}
