package playground.smetzler.bike;

import org.matsim.core.controler.AbstractModule;

public class BikeModule extends AbstractModule {

	@Override
	public void install() {
		addTravelTimeBinding("bike").to(BikeTravelTime.class);
		addTravelDisutilityFactoryBinding("bike").to(BikeTravelDisutilityFactory.class);
	//	addRoutingModuleBinding("bike").to();
	}

}
