package org.matsim.freight.carriers.consistency_checkers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.freight.carriers.*;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
 *
 *  @author antonstock
 *	This class checks, if all shipments can be transported -> vehicle has to be large enough and in operation during pickup/delivery times.
 *
 */
public class VehicleScheduleTest {

	private static final Logger log = LogManager.getLogger(VehicleScheduleTest.class);

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * TODO: @Anton: Bitte noch kurze Javadoc hier ergänzen.
	 *  Und bitte beachten, dass ich 2 Sachen umgebaut habe:
	 *  1.) Wie er an die Daten kommt : MATSimTestUtils genutzt...
	 *  2.) statt public static void main (...) habe ich eine die Methode umgenannt in testVehicleSchedule_passes( und mit @Test annotiert. So weiß JUnit, dass es sich um einen Test handelt.
	 *
	 *  @Anton: Bitte dann noch schauen, was ich im parallelen Test noch geschrieben habe.
	 *  Ansonsten wolltest du ja die Testmethode (und damit die obigen Records) noch "verschieben". Das habe ich hier noch nicht gemacht.
	 *  Gerne dann auhc zusehen, dass due die Record dann nicht mehr public hast. sondern idealerweise private.
	 *  Und auch beachten, was ich im CapacityCheck angemerkt habe: Bitte jeden Carrier einzeln überprüfen.
	 *
	 */
	@Test //TODO ab hier Kopien
	void testVehicleSchedule_passes() {

		// relative path to Freight/Scenarios/CCTestInput/
		String pathToInput = utils.getPackageInputDirectory();
		//names of xml-files
		String carriersXML = "CCTestCarriersShipmentsPASS.xml";
		String vehicleXML = "CCTestVeh.xml";

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup;
		freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);

		freightConfigGroup.setCarriersFile(pathToInput + carriersXML);
		freightConfigGroup.setCarriersVehicleTypesFile(pathToInput + vehicleXML);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		Carriers carriers = CarriersUtils.getCarriers(scenario);

		CarrierConsistencyCheckers.vehicleScheduleTest(carriers);

		//Assert...
	}

}
