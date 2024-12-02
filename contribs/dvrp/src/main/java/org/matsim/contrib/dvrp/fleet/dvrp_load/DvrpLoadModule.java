package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

/**
 * This module performs the default bindings related to {@link DvrpLoad}
 * @author Tarek Chouaki (tkchouaki)
 */
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
