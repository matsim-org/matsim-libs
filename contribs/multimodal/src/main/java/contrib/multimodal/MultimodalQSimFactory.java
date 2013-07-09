package contrib.multimodal;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalDepartureHandler;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngineFactory;
import org.matsim.core.router.util.TravelTime;

public class MultimodalQSimFactory implements MobsimFactory {

	private Map<String, TravelTime> multiModalTravelTimes;

	public MultimodalQSimFactory(Map<String, TravelTime> multiModalTravelTimes) {
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager);
		MultiModalSimEngine multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(qSim, multiModalTravelTimes);
		qSim.addMobsimEngine(multiModalEngine);
		qSim.addDepartureHandler(new MultiModalDepartureHandler(multiModalEngine, sc.getConfig().multiModal()));
		return qSim;
	}

}
