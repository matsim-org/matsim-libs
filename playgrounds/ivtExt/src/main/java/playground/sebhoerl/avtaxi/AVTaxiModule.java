package playground.sebhoerl.avtaxi;

import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerFactory;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.core.controler.AbstractModule;

public class AVTaxiModule extends AbstractModule {
	final static public String AV_MODE = "av";

	@Override
	public void install() {
		addRoutingModuleBinding(AV_MODE).toInstance(new DynRoutingModule(AV_MODE));
		bind(TaxiOptimizerFactory.class).to(DefaultTaxiOptimizerFactory.class);
	}
}
