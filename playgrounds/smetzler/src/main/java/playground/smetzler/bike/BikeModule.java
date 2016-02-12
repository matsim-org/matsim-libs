package playground.smetzler.bike;

import javax.inject.Inject;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouterFactoryModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;


public class BikeModule extends AbstractModule {

	@Override
	public void install() {
			
	//	Config config;
		
		// dient zur berenchung der traveltime abhaenging von verschiedneen parametern z.B surface, slope,...
		addTravelTimeBinding("bike").to(BikeTravelTime.class);
		//früher zb controler.add...
	//	BikeTravelTime.class.
		//bind(TravelTime.class).toInstance(new FreeSpeedTravelTime());

		//BikeTravelTime trvtime = new BikeTravelTime(this);
		
		// die Disutility wird aus time und distance mithilfe des RandomizingTimeDistanceTravelDisutility berechnet 
		// in den RandomizingTimeDistanceTravelDisutility/OnlyTimeDependentTravelDisutility sollte die brechntete aktuelle Fahrzeit aus der BikeTravelTime eingehen
		// keine ahnung wie das gehen soll, amit macht das irgendwie auch nicht explizit: PatnaSimulationTimeWriter AccessEgressMultimodalTripRouterModule
		// 
        addTravelDisutilityFactoryBinding("bike").toInstance( new RandomizingTimeDistanceTravelDisutility.Builder("bike"));
//        addTravelDisutilityFactoryBinding("bike").toInstance( new RandomizingTimeDistanceTravelDisutility.Builder("bike").createTravelDisutility(BikeTravelTime.class, getConfig().planCalcScore()));
        
//        addRoutingModuleBinding(mode)
       //addTravelDisutilityFactoryBinding("bike").to(BikeTravelDisutilityFactory.class);
		
		//von Multimodal Kopiert
       // addRoutingModuleBinding("bike").toProvider(new TripRouterFactoryModule.NetworkRoutingModuleProvider("bike"));


		//analog zu RunMobsimWithMultipleModeVehiclesExample, sorgt momentan lediglich dafür dass die maxV und PCU eingestellt ist.
		bindMobsim().toProvider(BikeQSimFactory.class);
		
		
	}

}
