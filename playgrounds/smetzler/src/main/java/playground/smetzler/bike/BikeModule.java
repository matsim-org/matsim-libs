package playground.smetzler.bike;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouterFactoryModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;


public class BikeModule extends AbstractModule {

	@Override
	public void install() {
		
		// dient zur berenchung der traveltime abhaenging von verschiedneen parametern z.B surface, slope,...
		addTravelTimeBinding("bike").to(BikeTravelTime.class);
		//früher zb controler.add...
		
		
		// die Disutility wird aus time und distance mithilfe des RandomizingTimeDistanceTravelDisutility berechnet 
        addTravelDisutilityFactoryBinding("bike").toInstance( new RandomizingTimeDistanceTravelDisutility.Builder( "bike" ) );
       //addTravelDisutilityFactoryBinding("bike").to(BikeTravelDisutilityFactory.class);
		
		//von Multimodal Kopiert
       // addRoutingModuleBinding("bike").toProvider(new TripRouterFactoryModule.NetworkRoutingModuleProvider("bike"));

        
		//analog zu RunMobsimWithMultipleModeVehiclesExample, sorgt momentan lediglich dafür dass die maxV und PCU eingestellt ist.
		bindMobsim().toProvider(BikeQSimFactory.class);
		
		
	}

}
