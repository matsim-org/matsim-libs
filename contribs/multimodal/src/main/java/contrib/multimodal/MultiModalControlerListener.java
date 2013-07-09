package contrib.multimodal;

import java.util.Map;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.TravelTime;

import contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import contrib.multimodal.tools.PrepareMultiModalScenario;

public class MultiModalControlerListener implements StartupListener {

	private Map<String, TravelTime> multiModalTravelTimes;

	@Override
	public void notifyStartup(StartupEvent event) {

	}

	
	
}
