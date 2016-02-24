package playground.artemc.socialCost;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

public class SocialCostControllerWithPT {


	public static void main(String[] args) {

		Controler controler = null;
		if (args.length == 0) {
			controler = new Controler(initSampleScenario());
		} else controler = new Controler(args);

		/*
		 * Scoring also has to take the social costs into account.
		 * This cannot be moved to the initializer since the scoring functions
		 * are created even before the startup event is created.
		 */
		//services.setScoringFunctionFactory(new TimeAndMoneyDependentScoringFunctionFactory());

		InitializerPT initializer = new InitializerPT();
		controler.addControlerListener(initializer);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}


	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig("H:/Work_Extern/SiouxFalls_Simulations/runInputs/config_PT_flowTollV2_local.xml");
		config.controler().setOutputDirectory("H:/Work_Extern/SiouxFalls_Simulations/PT_flowTollV2/");
		config.controler().setLastIteration(10);
		config.controler().setWritePlansInterval(10);
		config.controler().setWriteEventsInterval(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}

	/*
	 * Initialize the necessary components for the social cost calculation.
	 */
	public static class InitializerPT implements IterationStartsListener {
		
		private final double blendFactor;
		
		public InitializerPT(double blendFactor){
			this.blendFactor = blendFactor;			
		}
		
		public InitializerPT(){
			this.blendFactor = 0.1 ;			
		}
		

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			if(event.getIteration()==0){
				MatsimServices controler = event.getServices();

				VehicleOccupancyObserver vehicleObserver = new VehicleOccupancyObserver();
				
				// initialize the social costs calculator
                SocialCostCalculatorWithPT scc = new SocialCostCalculatorWithPT(controler.getScenario().getNetwork(), controler.getEvents(), controler.getLinkTravelTimes(), controler, vehicleObserver, blendFactor);
				
				controler.addControlerListener(scc);
				controler.getEvents().addHandler(scc);
			
				controler.getEvents().addHandler(vehicleObserver);

				// initialize the social costs disutility calculator
				final SocialCostTravelDisutilityFactory factory = new SocialCostTravelDisutilityFactory(scc, controler.getConfig().planCalcScore());
//				services.addOverridingModule(new AbstractModule() {
//					@Override
//					public void install() {
//						bindCarTravelDisutilityFactory().toInstance(factory);
//					}
//				});

				// create a plot containing the mean travel times
				Set<String> transportModes = new HashSet<String>();
				transportModes.add(TransportMode.car);
				transportModes.add(TransportMode.pt);
				transportModes.add(TransportMode.walk);
				MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
				controler.addControlerListener(mttc);
				controler.getEvents().addHandler(mttc);
				throw new RuntimeException();
			}
		}
	}

	private static class SocialCostTravelDisutility implements TravelDisutility {

		private final TravelTime travelTime;
		private final SocialCostCalculatorWithPT scc;
		private final double marginalCostOfDistance;
		private final double marginalCostOfTime;


		public SocialCostTravelDisutility(TravelTime travelTime, SocialCostCalculatorWithPT scc, PlanCalcScoreConfigGroup cnScoringGroup) {
			this.travelTime = travelTime;
			this.scc = scc;
			this.marginalCostOfTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
			this.marginalCostOfDistance = -cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney();
		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			double disutility = 0.0;
			disutility += this.marginalCostOfTime * this.travelTime.getLinkTravelTime(link, time, person, vehicle);
			disutility += this.marginalCostOfDistance * link.getLength();
			disutility += this.scc.getLinkTravelDisutility(link, time, person, vehicle); 
			return disutility;
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			throw new UnsupportedOperationException();
		}

	}

	private static class SocialCostTravelDisutilityFactory implements TravelDisutilityFactory {

		private final SocialCostCalculatorWithPT scc;
		private final PlanCalcScoreConfigGroup cnScoringGroup;

		public SocialCostTravelDisutilityFactory(SocialCostCalculatorWithPT scc, PlanCalcScoreConfigGroup cnScoringGroup) {
			this.scc = scc;
			this.cnScoringGroup = cnScoringGroup;
		}

		@Override
		public TravelDisutility createTravelDisutility(TravelTime travelTime) {
			return new SocialCostTravelDisutility(travelTime, scc, cnScoringGroup);
		}
	}

}