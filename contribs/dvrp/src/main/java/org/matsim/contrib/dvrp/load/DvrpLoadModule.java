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
		if (params.getDimensions().size() == 1) {
			bindModal(DvrpLoadType.class).toInstance(new IntegerLoadType(params.getDimensions().get(0)));
		} else {
			String[] dimensions = params.getDimensions().toArray(new String[params.getDimensions().size()]);
			bindModal(DvrpLoadType.class).toInstance(new IntegersLoadType(dimensions));
		}
	}
}
