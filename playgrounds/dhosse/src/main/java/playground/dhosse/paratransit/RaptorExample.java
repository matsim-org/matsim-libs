package playground.dhosse.paratransit;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.performance.raptor.Raptor;
import org.matsim.contrib.minibus.performance.raptor.RaptorDisutility;
import org.matsim.contrib.minibus.performance.raptor.TransitRouterQuadTree;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;

public class RaptorExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		PConfigGroup pConfig = new PConfigGroup();
		config.addModule(pConfig);
		ConfigUtils.loadConfig(config, "C:/Users/Daniel/Desktop/work/paratransit/testScenarioRaptor/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TransitRouterConfig trc = new TransitRouterConfig(config);
		
		TransitRouterImpl transitRouter = new TransitRouterImpl(trc, scenario.getTransitSchedule());
		
		RaptorDisutility raptorDisutility = new RaptorDisutility(trc, pConfig.getEarningsPerBoardingPassenger(), pConfig.getEarningsPerKilometerAndPassenger());
		TransitRouterQuadTree trq = new TransitRouterQuadTree(raptorDisutility);
		trq.initializeFromSchedule(scenario.getTransitSchedule(), trc.getBeelineWalkConnectionDistance());
		Raptor raptor = new Raptor(trq, raptorDisutility, trc);
		
		Person person = scenario.getPopulation().getPersons().values().iterator().next();
		Activity act1 = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		Activity act2 = (Activity) person.getSelectedPlan().getPlanElements().get(2);
		
		List<Leg> legListTransitRouter = transitRouter.calcRoute(act1.getCoord(), act2.getCoord(), act1.getEndTime(), person);
		List<Leg> legListRaptor = raptor.calcRoute(act1.getCoord(), act2.getCoord(), act1.getEndTime(), person);
		
		double ttimeTransit = 0.;
		StringBuffer transitRouteString = new StringBuffer();
		double ttimeRaptor = 0.;
		StringBuffer raptorRouteString = new StringBuffer();
		
		for(Leg leg : legListTransitRouter){
			
			ttimeTransit += leg.getTravelTime();
			
			if(leg.getRoute() != null){
				
				transitRouteString.append(leg.getRoute().getStartLinkId().toString() + 
				"_" + leg.getRoute().getEndLinkId().toString() + "===");
				
			}
			
		}
		
		for(Leg leg : legListRaptor){
			
			ttimeRaptor += leg.getTravelTime();
			
			if(leg.getRoute() != null){
				
				raptorRouteString.append(leg.getRoute().getStartLinkId().toString() + 
				"_" + leg.getRoute().getEndLinkId().toString() + "===");
				
			}
			
		}
		
		System.out.println(Time.writeTime(ttimeTransit, Time.TIMEFORMAT_HHMMSS) + "\t" + transitRouteString.toString());
		System.out.println(Time.writeTime(ttimeRaptor, Time.TIMEFORMAT_HHMMSS) + "\t" + raptorRouteString.toString());
		
	}

}
