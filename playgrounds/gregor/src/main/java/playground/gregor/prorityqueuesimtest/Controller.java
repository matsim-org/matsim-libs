package playground.gregor.prorityqueuesimtest;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.gregor.sim2d_v2.trafficmonitoring.MSATravelTimeCalculatorFactory;

public class Controller extends Controler {

	public Controller(String[] args) {
		super(args[0]);
		setOverwriteFiles(true);
//		this.config.addQSimConfigGroup(new QSimConfigGroup());
//		this.config.getQSimConfigGroup().setEndTime( 9*3600 + 5* 60);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		this.addMobsimFactory("prioQ",new PrioQMobsimFactory());
	}

	public Controller(Scenario sc) {
		super(sc);
		setOverwriteFiles(true);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		PrioQMobsimFactory factory = new PrioQMobsimFactory();
		this.addMobsimFactory("prioQ",factory);
	}


	@Override
	public PlanAlgorithm createRoutingAlgorithm(
			PersonalizableTravelCost travelCosts,
			PersonalizableTravelTime travelTimes) {
		PlansCalcRoute a = new PlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, getLeastCostPathCalculatorFactory(), ((PopulationFactoryImpl) this.scenarioData.getPopulation().getFactory()).getModeRouteFactory());
		a.addLegHandler("walkprioq", new NetworkLegRouter(this.network, a.getLeastCostPathCalculator(), a.getRouteFactory()));
		return a;
	}


	public static void main(String[] args) {

		String configFile = args[0];
		Config c = ConfigUtils.loadConfig(configFile);
//		c.addQSimConfigGroup(new QSimConfigGroup());
//		c.getQSimConfigGroup().setEndTime(  24*3600);

		Scenario sc = ScenarioUtils.createScenario(c);
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory("walkprioq", new LinkNetworkRouteFactory());
		ScenarioUtils.loadScenario(sc);

		Controler controller = new Controller(sc);
		controller.run();

	}
}
