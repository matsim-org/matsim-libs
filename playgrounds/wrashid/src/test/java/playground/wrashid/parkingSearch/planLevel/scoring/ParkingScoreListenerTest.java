package playground.wrashid.parkingSearch.planLevel.scoring;

import java.util.HashMap;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.parkingSearch.planLevel.BaseControlerScenario;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping1;

public class ParkingScoreListenerTest extends TestCase implements IterationEndsListener {

	public void testScenario() {
		Controler controler;
		String configFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessConfig3.xml";
		controler = new Controler(configFilePath);

		new BaseControlerScenario(controler);

		ParkingRoot.setParkingScoringFunction(new ParkingScoringFunctionTestNumberOfParkings(new ParkingPriceMapping1(),
				new IncomeRelevantForParking(), null));

		controler.addControlerListener(this);

		controler.run();
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		HashMap<Id, Double> hm = ParkingScoreListener.getScoreHashMap();
		assertEquals(-2.0, hm.get(new IdImpl(1)).doubleValue());
		assertEquals(-2.0, hm.get(new IdImpl(2)).doubleValue());
		assertEquals(-2.0, hm.get(new IdImpl(3)).doubleValue());
	}

}
