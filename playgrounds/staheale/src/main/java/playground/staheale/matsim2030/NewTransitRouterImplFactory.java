package playground.staheale.matsim2030;

import com.google.inject.Provider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.christoph.evacuation.pt.TransitRouterNetworkReaderMatsimV1;

public class NewTransitRouterImplFactory implements Provider<TransitRouter> {

	private final TransitSchedule schedule;
	private final TransitRouterConfig configTransit;
	private final TransitRouterNetwork routerNetwork;
	private final PreparedTransitSchedule preparedTransitSchedule;

	public NewTransitRouterImplFactory(final TransitSchedule schedule, final TransitRouterConfig configTransit, final Config config) {
		this.schedule = schedule;
		this.configTransit = configTransit;
		
		// read thinned transit router network from file
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
		TransitScheduleReader ScheduleReader = new TransitScheduleReader(sc); 
		ScheduleReader.readFile(config.transit().getTransitScheduleFile());
		TransitRouterNetwork transitRouterNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReaderMatsimV1(sc, transitRouterNetwork).parse("./input/run1/thinned_uvek2005network_adjusted.xml.gz");
		this.routerNetwork = transitRouterNetwork;
		
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
	}

	@Override
	public TransitRouter get() {
		TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.configTransit, this.preparedTransitSchedule);
		return new TransitRouterImpl(this.configTransit, new PreparedTransitSchedule(schedule), this.routerNetwork, ttCalculator, ttCalculator);
	}
	
}