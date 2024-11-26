package org.matsim.contrib.dvrp.fleet;

import org.matsim.contrib.dvrp.fleet.dvrp_load.DefaultIntegerLoadType;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

public class DvrpLoadModule extends AbstractDvrpModeModule {
	public DvrpLoadModule(String mode) {
		super(mode);
	}

	@Override
	public void install() {
		bindModal(IntegerLoadType.class).to(DefaultIntegerLoadType.class).asEagerSingleton();

		bindModal(DvrpLoadSerializer.class).toProvider(modalProvider(getter -> {
			IntegerLoadType integerLoadType = getter.getModal(IntegerLoadType.class);
			return new DefaultDvrpLoadSerializer(integerLoadType);
		}));
	}
}
