package contrib.multimodal;

import java.util.Map;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.PrepareMultiModalScenario;
import org.matsim.core.router.util.TravelTime;

public class MultiModalControlerListener implements StartupListener {

	private Map<String, TravelTime> multiModalTravelTimes;

	@Override
	public void notifyStartup(StartupEvent event) {

	}

	
	
}
