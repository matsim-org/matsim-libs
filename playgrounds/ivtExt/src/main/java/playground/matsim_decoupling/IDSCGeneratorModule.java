package playground.matsim_decoupling;

import org.matsim.core.controler.AbstractModule;

import ch.ethz.matsim.av.framework.AVUtils;

public class IDSCGeneratorModule extends AbstractModule {
	@Override
	public void install() {
        bind(RandomDensityGenerator.Factory.class);
        AVUtils.bindGeneratorFactory(binder(), "RandomDensity").to(RandomDensityGenerator.Factory.class);
	}
}
