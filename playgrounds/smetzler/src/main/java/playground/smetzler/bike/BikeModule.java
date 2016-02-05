package playground.smetzler.bike;

import org.matsim.core.controler.AbstractModule;


public class BikeModule extends AbstractModule {

	@Override
	public void install() {
		addTravelTimeBinding("bike").to(BikeTravelTime.class);
		//früher zb controler.add...
		addTravelDisutilityFactoryBinding("bike").to(BikeTravelDisutilityFactory.class);
		
		//analog zu RunMobsimWithMultipleModeVehiclesExample, sorgt momentan lediglich dafür dass die maxV und PCU eingestellt ist.
		bindMobsim().toProvider(BikeQSimFactory.class);
		
		
	//	addRoutingModuleBinding("bike").to();
	}

}
