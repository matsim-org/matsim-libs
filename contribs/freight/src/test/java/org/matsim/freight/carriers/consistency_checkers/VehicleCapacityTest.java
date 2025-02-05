package org.matsim.freight.carriers.consistency_checkers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.util.Assert;
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
 *	This class will check if the given vehicles have enough capacity to meet the capacity demand (size) of given shipments.
 *	Please change the input path and names of xml files below.
 */
public class VehicleCapacityTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * TODO: @Anton: Bitte noch kurze Javadoc hier ergänzen.
	 *  Und bitte beachten, dass ich 2 Sachen umgebaut habe:
	 *  1.) Wie er an die Daten kommt : MATSimTestUtils genutzt...
	 *  2.) statt public static void main (...) habe ich eine die MEhtode umgenannt in testVehicleCapacity und mit @Test annotiert. So weiß JUnit, dass es sich um einen Test handelt.
	 */
	@Test //TODO hier Kopien anlegen (verschiedene Ausgänge etc)
	void testVehicleCapacity_passes() {

		Config config = ConfigUtils.createConfig();

		FreightCarriersConfigGroup freightConfigGroup = addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfigGroup.setCarriersFile(utils.getPackageInputDirectory() + "CCTestCarriers.xml");
		freightConfigGroup.setCarriersVehicleTypesFile(utils.getPackageInputDirectory() + "CCTestVeh.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//load carriers according to freight config
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.getCarriers(scenario);
		/**
		* System.out.println("Starting 'IsVehicleBigEnoughTest'...");
		*/

		CarrierConsistencyCheckers.capacityCheck(carriers);
    // TODO: @Anton: Wäre dann vielleicht schön, wenn der Test mindestens einen Boolean - Wert
    // (TRUE, FALSE) zurückgäbe mit dem Ergebenis.
    // Dann könntest du nun hier gegen diesen Wert prüfen, ob der Test erfolgreich war oder nicht. ! Das ist nur um zu sehen, dass deine Tests das machen was sie sollen
	// und wird nicht später im Produktiveinsatz ausgegeben. Ist nur die Rückversicherung, dass da niemand deine Checks umgebaut hat und sie nun nicht mehr funktionieren.
    // Assert.isTrue( wertDesTests ); // Ohne weitere Ausgabe von Hinweisen, was mit dem Test ist.
	// Assert.isTrue( wertDesTests , "Fehlermeldung"); // Ohne weitere Ausgabe von Hinweisen, was mit dem Test ist.
	}

	//Todo: @Anton: Wenn du das mit der Rückmeldung hast, kannst du natürlich das nochmal analog mit nem Carrier / Vehicle machen, wo der Check nicht erfolgreich ist.
	//-> Eigene Test Methode
	// Wäre dann die Abfrage Assert.isFalse( wertDesTests );

}

