package playground.artemc.socialCost;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

public class SocialCostController {


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
		//controler.setScoringFunctionFactory(new TimeAndMoneyDependentScoringFunctionFactory());

		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		controler.setOverwriteFiles(true);
		controler.run();
	}


	private static Scenario initSampleScenario() {

		Config config = ConfigUtils.loadConfig("C:/Workspace/roadpricingSingapore/scenarios/siouxFalls/config.xml");
		//config.controler().setOutputDirectory("./outpout_SiouxFalls/pt_UE_7200_calibration_PT_6min_600m");
		//config.controler().setOutputDirectory("./outpout_SiouxFalls/SiouxFalls_5PT_Lines");
		config.controler().setOutputDirectory("C:/Workspace/roadpricingSingapore/output_SiouxFalls/sf_10it_withPT");
		config.controler().setLastIteration(10);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		return scenario;
	}

	/*
	 * Initialize the necessary components for the social cost calculation.
	 */
	public static class Initializer implements IterationStartsListener {

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			if(event.getIteration()==0){
				Controler controler = event.getControler();

				// initialize the social costs calculator
				SocialCostCalculator scc = new SocialCostCalculator(controler.getNetwork(), controler.getEvents(), controler.getLinkTravelTimes(), controler);
				
				controler.addControlerListener(scc);
				controler.getEvents().addHandler(scc);

				// initialize the social costs disutility calculator
				SocialCostTravelDisutilityFactory factory = new SocialCostTravelDisutilityFactory(scc);
				controler.setTravelDisutilityFactory(factory);

				// create a plot containing the mean travel times
				Set<String> transportModes = new HashSet<String>();
				transportModes.add(TransportMode.car);
				MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
				controler.addControlerListener(mttc);
				controler.getEvents().addHandler(mttc);
			}
		}
	}

	private static class SocialCostTravelDisutility implements TravelDisutility {

		private final TravelTime travelTime;
		private final SocialCostCalculator scc;
		private final double marginalCostOfDistance;
		private final double marginalCostOfTime;


		public SocialCostTravelDisutility(TravelTime travelTime, SocialCostCalculator scc, PlanCalcScoreConfigGroup cnScoringGroup) {
			this.travelTime = travelTime;
			this.scc = scc;
			this.marginalCostOfTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
			this.marginalCostOfDistance = - cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney();
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

		private final SocialCostCalculator scc;

		public SocialCostTravelDisutilityFactory(SocialCostCalculator scc) {
			this.scc = scc;
		}

		@Override
		public TravelDisutility createTravelDisutility(TravelTime travelTime, PlanCalcScoreConfigGroup cnScoringGroup) {
			return new SocialCostTravelDisutility(travelTime, scc, cnScoringGroup);
		}
	}

}