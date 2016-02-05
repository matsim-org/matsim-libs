package playground.smetzler.bike;

import org.matsim.core.controler.AbstractModule;


public class BikeModule extends AbstractModule {

	@Override
	public void install() {
		addTravelTimeBinding("bike").to(BikeTravelTime.class);
		//früher zb controler.add...
		addTravelDisutilityFactoryBinding("bike").to(BikeTravelDisutilityFactory.class);
		
		//analog zu RunMobsimWithMultipleModeVehiclesExample, brauch ich das überhaupt? was brauch ich überhaupt?
		bindMobsim().toProvider(BikeQSimFactory.class);
		
		
	//	addRoutingModuleBinding("bike").to();
	}

}
