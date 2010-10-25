package playground.mzilske.deteval;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;

public class RunDetailedEvaluation {

	private static final String configFilename = "../../detailedEval/pop/befragte-personen/config.xml";
	
	private static final String backgroundPopFilename = "../../detailedEval/pop/gueterVerkehr/population_gv_bavaria_1pct_wgs84.xml.gz";
	
	private CoordinateTransformation wgs84ToDhdnGk4 = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4); 
	
	public void run() {
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFilename);
		config.scenario().setUseTransit(false);
		config.scenario().setUseVehicles(true);
		
		final ScenarioImpl scenario = new ScenarioImpl(config);
		new ScenarioLoaderImpl(scenario).loadScenario();
		
		ScenarioImpl backgroundScenario = new ScenarioImpl(config);
		new MatsimNetworkReader(backgroundScenario).readFile(config.network().getInputFile());
		new MatsimPopulationReader(backgroundScenario).readFile(backgroundPopFilename);

	
		ParallelPersonAlgorithmRunner.run(backgroundScenario.getPopulation(), 1,
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new AbstractPersonAlgorithm() {

					@Override
					public void run(Person person) {
						for (Plan plan : person.getPlans()) {
							for (PlanElement planElement : plan.getPlanElements()) {
								if (planElement instanceof Activity) {
									Activity activity = (Activity) planElement;
									((ActivityImpl) activity).setCoord(wgs84ToDhdnGk4.transform(activity.getCoord()));
								}
								if (planElement instanceof Leg) {
									
								}
							}
						}
					}
					
				};
			}
		});
		
		DijkstraFactory leastCostPathCalculatorFactory = new DijkstraFactory();
		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
		TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());
		final PlansCalcRoute routingAlgorithm = new PlansCalcRoute(config.plansCalcRoute(), scenario.getNetwork(), travelCostCalculatorFactory.createTravelCostCalculator(travelTimeCalculator, config.charyparNagelScoring()), travelTimeCalculator, leastCostPathCalculatorFactory);
		ParallelPersonAlgorithmRunner.run(backgroundScenario.getPopulation(), 1,
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(routingAlgorithm, scenario.getNetwork());
			}
		});
		
		// new PopulationWriter(backgroundScenario.getPopulation(), backgroundScenario.getNetwork()).write("./output/detailedEval/hintergrund-pop.xml");
		
		
		new CarAssigner(scenario.getPopulation(), scenario.getVehicles()).run();
		new CarAssigner(backgroundScenario.getPopulation(), scenario.getVehicles()).run();
		
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		DetailedEvaluationMobsimFactory mobsimFactory = new DetailedEvaluationMobsimFactory(0);
		mobsimFactory.addBackgroundPopulation(backgroundScenario.getPopulation());
		mobsimFactory.setTeleportedModes(new String[] {TransportMode.bike, TransportMode.pt, TransportMode.ride, TransportMode.transit_walk, TransportMode.walk, "undefined"});
		mobsimFactory.setUseOTFVis(true);
		controler.setMobsimFactory(mobsimFactory);
		
		controler.run();
	}
	
	public static void main(String[] args) {
		RunDetailedEvaluation run = new RunDetailedEvaluation();
		run.run();
	}
	
}
