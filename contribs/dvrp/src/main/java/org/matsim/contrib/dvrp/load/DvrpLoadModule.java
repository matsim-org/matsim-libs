package org.matsim.contrib.dvrp.load;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;

/**
 * This module performs the default bindings related to {@link DvrpLoad}
 * 
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public class DvrpLoadModule extends AbstractDvrpModeModule {
	private final DvrpLoadParams params;

	public DvrpLoadModule(String mode, DvrpLoadParams params) {
		super(mode);
		this.params = params;
	}

	@Override
	public void install() {
		if (params.dimensions.size() == 1) {
			bindModal(DvrpLoadType.class).toInstance(new IntegerLoadType(params.dimensions.get(0)));
		} else {
			String[] dimensions = params.dimensions.toArray(new String[params.dimensions.size()]);
			bindModal(DvrpLoadType.class).toInstance(new IntegersLoadType(dimensions));
		}
	}
}
