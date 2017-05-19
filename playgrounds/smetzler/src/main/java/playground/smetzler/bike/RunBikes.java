package playground.smetzler.bike;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class RunBikes {
	
	public static void main(String[] args) {

//		//boddin
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/boddin/config_bike_boddin.xml", new BikeConfigGroup());
//		//berlin
//		Config config = ConfigUtils.loadConfig("../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/config_bike_berlin.xml", new BikeConfigGroup());

		//Oslo
		Config config = ConfigUtils.loadConfig("../../../../desktop/Oslo/config_bike_oslo.xml", new BikeConfigGroup());

		
//		config.controler().setOutputDirectory("../../../runs-svn/berlin-bike/BerlinBike_0804_BVG_15000");
//		
//		config.plans().setInputFile("demand/bvg.run189.10pct.100.plans.selected_bikeonly_1percent_clean.xml.gz" );
//		
//		config.network().setInputFile("network/BerlinBikeNet_MATsim.xml");
		

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		config.global().setNumberOfThreads(1);
		
		config.controler().setLastIteration(0);
		
//		calculate customized bike speed per link? makes separate network unnecessary 
//		config.qsim().setLinkDynamics( LinkDynamics.PassingQ.name() );
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
//		 BEGIN_NEW : following is added to use the functionality using scenario vehicles rather than adding to population agent source. If something doesnot work, let me know. Amit May'17
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(30.0/3.6);
		bike.setPcuEquivalents(0.0);
		scenario.getVehicles().addVehicleType(bike);

		scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
//		END_NEW

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new BikeModule());
		controler.run();
	}

}