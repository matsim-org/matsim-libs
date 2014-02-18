package playground.mzilske.cdr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.contrib.cadyts.general.ExpBetaPlanSelectorWithCadytsPlanRegistration;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar.Selector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;

import com.telmomenezes.jfastemd.Feature;
import com.telmomenezes.jfastemd.JFastEMD;
import com.telmomenezes.jfastemd.Signature;

import d4d.Sighting;


//		double varianceScale = 0.1;
// matsimCalibrator.setVarianceScale(varianceScale);
// matsimCalibrator.setMinStddev(25.*varianceScale, TYPE.FLOW_VEH_H);
// matsimCalibrator.setMinStddev(1, TYPE.COUNT_VEH);

public class CompareMain {
	
	private String suffix = "";
	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	private static final class ZeroScoringFunctionFactory implements
			ScoringFunctionFactory {
		@Override
		public ScoringFunction createNewScoringFunction(Plan plan) {
			return new ScoringFunction() {

				@Override
				public void handleActivity(Activity activity) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void handleLeg(Leg leg) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void agentStuck(double time) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void addMoney(double amount) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void finish() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public double getScore() {
					return 0;
				}

				@Override
				public void handleEvent(Event event) {
					// TODO Auto-generated method stub
					
				}
				
			};
		}
	}

	static class Result {
		
		VolumesAnalyzer volumes;
		
		Scenario scenario;

		CadytsContext context;
		
	}

	private static class VolumeOnLinkFeature implements Feature {

		final Link link;
		final int hour;

		public VolumeOnLinkFeature(Link link, int i) {
			this.link = link;
			this.hour = i;
		}

		@Override
		public double groundDist(Feature f) {
			VolumeOnLinkFeature other = (VolumeOnLinkFeature) f;
			double spacialDistance = CoordUtils.calcDistance(link.getCoord(), other.link.getCoord());
			int temporalDistance = Math.abs(hour - other.hour);
			return (0.0 * spacialDistance) + (2.0 * temporalDistance);
		}

	}

	private static final int TIME_BIN_SIZE = 60*60;
	private static final int MAX_TIME = 24 * TIME_BIN_SIZE - 1;
	private static final int dailyRate = 0;
	private CallProcessTicker ticker;
	private CallProcess callProcess;
	private VolumesAnalyzer groundTruthVolumes;
	private LinkToZoneResolver linkToZoneResolver;
	private Scenario scenario;
	private VolumesAnalyzer cdrVolumes;

	public void runWithTwoPlansAndCadyts() {
		ticker.finish();
		callProcess.dump();
		List<Sighting> sightings = callProcess.getSightings();

		Counts counts = volumesToCounts(groundTruthVolumes);
		cdrVolumes = runWithTwoPlansAndCadyts(scenario.getNetwork(), linkToZoneResolver, sightings, counts);
	}
	
	public Result runWithOnePlanAndCadyts() {
		ticker.finish();
		callProcess.dump();
		List<Sighting> sightings = callProcess.getSightings();

		Counts counts = volumesToCounts(groundTruthVolumes);
		Result result = runWithOnePlanAndCadyts(scenario.getNetwork(), linkToZoneResolver, sightings, counts);
		cdrVolumes = result.volumes;
		return result;
	}
	
	public Result runWithOnePlanAndCadytsAndInflation() {
		ticker.finish();
		callProcess.dump();
		List<Sighting> sightings = callProcess.getSightings();

		Counts counts = volumesToCounts(groundTruthVolumes);
		Result result = runWithOnePlanAndCadytsAndInflation(scenario.getNetwork(), linkToZoneResolver, sightings, counts);
		cdrVolumes = result.volumes;
		return result;
	}
	
	public void runOnceWithSimplePlansUnCongested(Config config) {
		ticker.finish();
		callProcess.dump();
		List<Sighting> sightings = callProcess.getSightings();

		cdrVolumes = runOnceWithSimplePlansUncongested(config, scenario.getNetwork(), linkToZoneResolver, sightings);
	}
	
	public void runOnceWithSimplePlans(Config config) {
		ticker.finish();
		callProcess.dump();
		List<Sighting> sightings = callProcess.getSightings();
		cdrVolumes = runOnceWithSimplePlans(config, scenario.getNetwork(), linkToZoneResolver, sightings, suffix);
	}

	double compareEMD() {
		Network network = scenario.getNetwork();
		Signature signature1 = makeSignature(network, groundTruthVolumes);
		Signature signature2 = makeSignature(network, cdrVolumes);
		double emd = JFastEMD.distance(signature1, signature2, -1.0);
		return emd;
	}

	double compareTimebins() {
		double sum = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			int[] volumesForLink1 = getVolumesForLink(groundTruthVolumes, link);
			int[] volumesForLink2 = getVolumesForLink(cdrVolumes, link);
			for (int i = 0; i < volumesForLink1.length; ++i) {
				int diff = volumesForLink1[i] - volumesForLink2[i];
				sum += Math.abs(diff) * link.getLength();
				if (diff != 0) {
					System.out.println(Arrays.toString(volumesForLink1));
					System.out.println(Arrays.toString(volumesForLink2));
					System.out.println("=== " + link.getId());
				}
			}
		}
		return sum;
	}

	double compareAllDay() {
		double sum = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			int[] volumesForLink1 = getVolumesForLink(groundTruthVolumes, link);
			int[] volumesForLink2 = getVolumesForLink(cdrVolumes, link);
			int sum1 = 0;
			int sum2 = 0;
			for (int i = 0; i < volumesForLink1.length; ++i) {
				sum1 += volumesForLink1[i];
				sum2 += volumesForLink2[i];
			}
			int diff = sum2 - sum1;
			sum += Math.abs(diff) * link.getLength();
		}
		return sum;
	}

	double compareEMDMassPerLink() {
		double sum = 0;
		double lengthsum = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			int[] volumesForLink1 = getVolumesForLink(groundTruthVolumes, link);
			int[] volumesForLink2 = getVolumesForLink(cdrVolumes, link);
			double emd =  MatchDistance.emd(MatchDistance.int2double(volumesForLink1), MatchDistance.int2double(volumesForLink2));
			if (! Double.isNaN(emd)) {
				lengthsum += link.getLength();
				sum += link.getLength() *emd;
			}
		}
		return sum / lengthsum;
	}

	private Counts volumesToCounts(VolumesAnalyzer volumesAnalyzer) {
		Counts counts = new Counts();
		Network network = scenario.getNetwork();
		for (Link link : network.getLinks().values()) {
			Count count = counts.createAndAddCount(link.getId(), link.getId().toString());
			int[] volumesForLink = getVolumesForLink(volumesAnalyzer, link);
			int h = 1;
			for (int v : volumesForLink) {
				count.createVolume(h, v);
				++h;
			}
		}
		return counts;
	}

	CompareMain(Scenario scenario, EventsManager events, CallBehavior callingBehavior) {
		super();
		this.scenario = scenario;

		Map<Id, Id> initialPersonInZone = new HashMap<Id, Id>();


		// final Zones cellularCoverage = SyntheticCellTowerDistribution.naive(scenario.getNetwork());

		LinkToZoneResolver trivialLinkToZoneResolver = new LinkToZoneResolver() {

			@Override
			public Id resolveLinkToZone(Id linkId) {
				return linkId;	
			}

			public IdImpl chooseLinkInZone(String zoneId) {
				return new IdImpl(zoneId);
			}

		};

		linkToZoneResolver = trivialLinkToZoneResolver;

		// linkToZoneResolver = new CellularCoverageLinkToZoneResolver(cellularCoverage, scenario.getNetwork());

		for (Person p : scenario.getPopulation().getPersons().values()) {
			Id linkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
			System.out.println(linkId);
			initialPersonInZone.put(p.getId(), linkToZoneResolver.resolveLinkToZone(linkId));
		}

		ticker = new CallProcessTicker();
		events.addHandler(ticker);


		ZoneTracker zoneTracker = new ZoneTracker(events, linkToZoneResolver, initialPersonInZone);
		callProcess = new CallProcess(null, scenario.getPopulation(), zoneTracker, callingBehavior);
		ticker.addHandler(zoneTracker);

		ticker.addHandler(callProcess);
		ticker.addSteppable(callProcess);

		groundTruthVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, scenario.getNetwork());
		events.addHandler(groundTruthVolumes);
	}

	private static Signature makeSignature(Network network, VolumesAnalyzer volumesAnalyzer) {
		int n = 0;
		for (Link link : network.getLinks().values()) {
			int[] volumesForLink = getVolumesForLink(volumesAnalyzer, link);
			for (int i=0; i<volumesForLink.length;++i) {
				if (volumesForLink[i] != 0) {
					++n;
				}
			}
		}
		Feature[] features = new Feature[n];
		double[] weights = new double[n];
		n = 0;
		for (Link link : network.getLinks().values()) {
			int[] volumesForLink = getVolumesForLink(volumesAnalyzer, link);
			for (int i=0; i<volumesForLink.length;++i) {
				if (volumesForLink[i] != 0) {
					Feature feature = new VolumeOnLinkFeature(link, i);
					features[n] = feature;
					weights[n] = volumesForLink[i];
					++n;
				}
			}
		}
		System.out.println(n);
		Signature signature = new Signature();
		signature.setFeatures(features);
		signature.setNumberOfFeatures(n);
		signature.setWeights(weights);
		return signature;
	}

	public static VolumesAnalyzer runWithTwoPlansAndCadyts(Network network, final LinkToZoneResolver linkToZoneResolver, List<Sighting> sightings, Counts counts) {
		Config config = ConfigUtils.createConfig();
		ActivityParams sightingParam = new ActivityParams("sighting");
		// sighting.setOpeningTime(0.0);
		// sighting.setClosingTime(0.0);
		sightingParam.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sightingParam);
		config.planCalcScore().setTraveling_utils_hr(-6);
		config.planCalcScore().setPerforming_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(-6);
		config.planCalcScore().setConstantCar(0);
		config.planCalcScore().setMonetaryDistanceCostRateCar(0);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(10);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);

		StrategySettings stratSets = new StrategySettings(new IdImpl(1));
		stratSets.setModuleName("ccc") ;
		stratSets.setProbability(1.) ;
		config.strategy().addStrategySettings(stratSets) ;


		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);



		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);

			}
			System.out.println(sighting.getCellTowerId().toString());

			sightingsOfPerson.add(sighting);
		}


		PopulationFromSightings.createPopulationWithTwoPlansEach(scenario2, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		final CadytsContext context = new CadytsContext(config, counts) ;
		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(context);
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				CadytsPlanChanger planSelector = new CadytsPlanChanger(scenario2,context);
				planSelector.setCadytsWeight(10000000);
				return new PlanStrategyImpl(planSelector);
			}} ) ;
		controler.setCreateGraphs(false);
		controler.run();
		double sum=0.0;
		for (Person person : scenario2.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			double currentPlanCadytsCorrection = calcCadytsScore(context, plan);
			sum += Math.abs(currentPlanCadytsCorrection);
		}
		System.out.println(sum);
		return controler.getVolumes();
	}

	public static Result runWithOnePlanAndCadyts(Network network, final LinkToZoneResolver linkToZoneResolver, List<Sighting> sightings, Counts counts) {
		Config config = ConfigUtils.createConfig();
		ActivityParams sightingParam = new ActivityParams("sighting");
		sightingParam.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sightingParam);
		config.planCalcScore().setTraveling_utils_hr(-6);
		config.planCalcScore().setPerforming_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(-6);
		config.planCalcScore().setConstantCar(0);
		config.planCalcScore().setMonetaryDistanceCostRateCar(0);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(100);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		
		StrategySettings stratSets = new StrategySettings(new IdImpl(1));
		stratSets.setModuleName("ccc") ;
		stratSets.setProbability(1.) ;
		config.strategy().addStrategySettings(stratSets) ;

		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);



		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);

			}
			System.out.println(sighting.getCellTowerId().toString());

			sightingsOfPerson.add(sighting);
		}


		PopulationFromSightings.createPopulationWithEndTimesAtLastSightingsAndStayAtHomePlan(scenario2, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		final CadytsContext context = new CadytsContext(config, counts) ;
		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);
		controler.setScoringFunctionFactory(new ZeroScoringFunctionFactory());
		controler.addControlerListener(context);
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				// KeepSelected planSelector = new KeepSelected();
				CadytsPlanChanger planSelector = new CadytsPlanChanger(scenario2,context);
				planSelector.setCadytsWeight(10000000);
				return new PlanStrategyImpl(planSelector);
			}} ) ;
		controler.setCreateGraphs(false);
		controler.run();
		
		Result result = new Result();
		result.volumes = controler.getVolumes();
		result.scenario = scenario2;
		result.context = context;
		return result;
	}
	
	public static Result runWithOnePlanAndCadytsAndInflation(Network network, final LinkToZoneResolver linkToZoneResolver, List<Sighting> sightings, Counts counts) {
		final Config config = ConfigUtils.createConfig();
		ActivityParams sightingParam = new ActivityParams("sighting");
		sightingParam.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sightingParam);
		config.planCalcScore().setTraveling_utils_hr(-6);
		config.planCalcScore().setPerforming_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(-6);
		config.planCalcScore().setConstantCar(0);
		config.planCalcScore().setMonetaryDistanceCostRateCar(0);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(300);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		
		StrategySettings stratSets = new StrategySettings(new IdImpl(1));
		stratSets.setModuleName("ccc") ;
		stratSets.setProbability(1.) ;
		config.strategy().addStrategySettings(stratSets) ;
		StrategySettings random = new StrategySettings(new IdImpl(2));
		random.setModuleName(Selector.SelectRandom.toString()) ;
		random.setProbability(0.1) ;
		config.strategy().addStrategySettings(random) ;

		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);

 

		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);

			}
			System.out.println(sighting.getCellTowerId().toString());

			sightingsOfPerson.add(sighting);
		}


		PopulationFromSightings.createPopulationWithEndTimesAtLastSightingsAndAdditionalInflationPopulation(scenario2, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		final CadytsContext context = new CadytsContext(config, counts) ;
	
		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(context);
		// controler.setScoringFunctionFactory(new ZeroScoringFunctionFactory());
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(plan, config, context);
		//		scoringFunction.setWeightOfCadytsCorrection(10.0);
				sumScoringFunction.addScoringFunction(scoringFunction);
				return sumScoringFunction;
			}
			
		});
		controler.addPlanStrategyFactory("ccc", new PlanStrategyFactory() {
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager events2) {
				// KeepSelected planSelector = new KeepSelected();
				// CadytsPlanChanger planSelector = new CadytsPlanChanger(scenario2,context);
				// planSelector.setCadytsWeight(100000);
				// PlanSelectionByCadyts<Link> planSelector = new PlanSelectionByCadyts<Link>(1.0, context);
				ExpBetaPlanSelectorWithCadytsPlanRegistration planSelector = new ExpBetaPlanSelectorWithCadytsPlanRegistration(1.0, context);
				return new PlanStrategyImpl(planSelector);
			}} ) ;
		controler.setCreateGraphs(false);
		controler.run();
		
		Result result = new Result();
		result.volumes = controler.getVolumes();
		result.scenario = scenario2;
		result.context = context;
		return result;
	}

	public static double calcCadytsScore(final CadytsContext context, Plan plan) {
		cadyts.demand.Plan<Link> currentPlanSteps = context.getPlansTranslator().getPlanSteps(plan);
		double currentPlanCadytsCorrection = context.getCalibrator().calcLinearPlanEffect(currentPlanSteps); // / this.beta;
		// CadytsPlanUtils.printCadytsPlan(currentPlanSteps);
		return currentPlanCadytsCorrection;
	}
	
	public static VolumesAnalyzer runOnceWithSimplePlansUncongested(Config config, Network network, final LinkToZoneResolver linkToZoneResolver, List<Sighting> sightings) {


		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);

		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);

			}
			System.out.println(sighting.getCellTowerId().toString());

			sightingsOfPerson.add(sighting);
		}


		PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario2, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);

		controler.setCreateGraphs(false);
		controler.run();
		return controler.getVolumes();
	}
	
	public static VolumesAnalyzer runWithSimplePlans(Config config, Network network, final LinkToZoneResolver linkToZoneResolver, List<Sighting> sightings, String suffix) {
		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);

		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);

			}
			System.out.println(sighting.getCellTowerId().toString());

			sightingsOfPerson.add(sighting);
		}

		PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario2, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();
		return controler.getVolumes();
	}
	
	public static VolumesAnalyzer runOnceWithSimplePlans(Config config, Network network, final LinkToZoneResolver linkToZoneResolver, List<Sighting> sightings, String suffix) {
		

		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);

		final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
		for (Sighting sighting : sightings) {

			List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
			if (sightingsOfPerson == null) {
				sightingsOfPerson = new ArrayList<Sighting>();
				allSightings.put(sighting.getAgentId(), sightingsOfPerson);

			}
			System.out.println(sighting.getCellTowerId().toString());

			sightingsOfPerson.add(sighting);
		}


		PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario2, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		Controler controler = new Controler(scenario2);
		controler.setOverwriteFiles(true);

		controler.setCreateGraphs(false);
		controler.run();
		return controler.getVolumes();
	}

	public static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Link link) {
		int maxSlotIndex = (MAX_TIME / TIME_BIN_SIZE) + 1;
		int[] maybeVolumes = volumesAnalyzer1.getVolumesForLink(link.getId());
		if(maybeVolumes == null) {
			return new int[maxSlotIndex + 1];
		}
		return maybeVolumes;
	}

	public static VolumesAnalyzer calculateVolumes(Network network, String eventsFilename) {
		VolumesAnalyzer volumesAnalyzer = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, network);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(volumesAnalyzer);
		new MatsimEventsReader(events).readFile(eventsFilename);
		return volumesAnalyzer;
	}

}
