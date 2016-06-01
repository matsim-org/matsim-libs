package playground.smetzler.bike;


import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.PrepareForSim;


public class BikeModule extends AbstractModule {

	@Override
	public void install() {

//		bind(PrepareForSim.class).to(BikePrepareForSimImpl.class);
		addTravelTimeBinding("bike").to(BikeTravelTime.class);

		addTravelDisutilityFactoryBinding("bike").to(BikeTravelDisutilityFactory.class);
//		addTravelDisutilityFactoryBinding("car").toInstance( new RandomizingTimeDistanceTravelDisutility.Builder("car"));

		//analog zu RunMobsimWithMultipleModeVehiclesExample, sorgt momentan lediglich dafuer dass die maxSpeed und PCU eingestellt ist.
		
		//fuer equil ohne qsim
		bindMobsim().toProvider(BikeQSimFactory.class);
	}
}





///////////////////////////////////////////////// ALTER CODE ////////////////////////////////////////////////
//@Override
//public void install() {
//				
//	// dient zur berenchung der traveltime abhaenging von verschiedneen parametern z.B surface, slope,...
//	addTravelTimeBinding("bike").to(BikeTravelTime.class);
//	//früher zb controler.add...
//
//	
//	// die Disutility wird aus time und distance mithilfe des RandomizingTimeDistanceTravelDisutility berechnet 
//	// in den RandomizingTimeDistanceTravelDisutility/OnlyTimeDependentTravelDisutility sollte die brechntete aktuelle Fahrzeit aus der BikeTravelTime eingehen
//	// keine ahnung wie das gehen soll, amit macht das irgendwie auch nicht explizit: PatnaSimulationTimeWriter AccessEgressMultimodalTripRouterModule
//	// beispiel fuer disutility: SlopeAwareTravelDisutilityFactory
//    addTravelDisutilityFactoryBinding("bike").toInstance( new RandomizingTimeDistanceTravelDisutility.Builder("bike"));
////     addTravelDisutilityFactoryBinding("bike").toInstance( new RandomizingTimeDistanceTravelDisutility.Builder("bike").setSigma(val);
//    
//
//    
//	//analog zu RunMobsimWithMultipleModeVehiclesExample, sorgt momentan lediglich dafür dass die maxV und PCU eingestellt ist.
//	bindMobsim().toProvider(BikeQSimFactory.class);
//	
//	
//}