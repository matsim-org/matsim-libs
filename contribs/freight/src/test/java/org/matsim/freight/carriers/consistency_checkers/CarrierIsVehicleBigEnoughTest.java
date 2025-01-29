	package org.matsim.freight.carriers.consistency_checkers;

	import org.matsim.api.core.v01.Scenario;
	import org.matsim.core.config.ConfigUtils;
	import org.matsim.core.controler.Controler;
	import org.matsim.core.scenario.ScenarioUtils;
	import org.matsim.freight.carriers.*;
	import org.matsim.core.config.Config;

	import static org.matsim.core.controler.Controler.DefaultFiles.config;

	/**
	 *
	 *  @author antonstock
	 *
	 */
public class CarrierIsVehicleBigEnoughTest {
		public static void main(String[] args){

			//Relativer Pfad zu Freight/Scenarios/CCTestInput/
			String pathToInput = "../";
			Config config = ConfigUtils.createConfig();

			if ( args==null || args.length==0 || args[0]==null ){

				FreightCarriersConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class );
				freightConfigGroup.setCarriersFile(  pathToInput + "CCTestCarriers.xml" );
				freightConfigGroup.setCarriersVehicleTypesFile( pathToInput + "CCTestVeh.xml" );
			} else {
				config = ConfigUtils.loadConfig( args, new FreightCarriersConfigGroup() );
			}

			System.out.println("Starting 'IsVehicleBigEnoughTest'...");

			Scenario scenario = ScenarioUtils.loadScenario( config );
			Carriers carriers = CarriersUtils.getCarriers(scenario);

			for (Carrier carrier : carriers.getCarriers().values()) {

				for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {

					var capacity = carrierVehicle.getType().getCapacity().getOther();

						System.out.println("Carrier ID: " + carrier.getId()+"Other Vehicle capacity: " + capacity);
				}
			}
		}
}
