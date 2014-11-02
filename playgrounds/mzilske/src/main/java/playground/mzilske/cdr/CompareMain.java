package playground.mzilske.cdr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;
import playground.mzilske.cdr.ZoneTracker.Zone;

import com.telmomenezes.jfastemd.Feature;
import com.telmomenezes.jfastemd.JFastEMD;
import com.telmomenezes.jfastemd.Signature;

@Singleton
public class CompareMain {

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
    private CallProcessTicker ticker;
	private CallProcess callProcess;

    public VolumesAnalyzer getGroundTruthVolumes() {
        return groundTruthVolumes;
    }

    private VolumesAnalyzer groundTruthVolumes;
	private LinkToZoneResolver linkToZoneResolver;
	private Scenario scenario;

    public ScenarioImpl createScenarioFromSightings(Config config) {
		final ScenarioImpl scenario21 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario21.setNetwork(scenario.getNetwork());

        final Sightings allSightings = new SightingsImpl(getSightingsPerPerson());
		
		
		PopulationFromSightings.createPopulationWithEndTimesAtLastSightings(scenario21, linkToZoneResolver, allSightings);
		PopulationFromSightings.preparePopulation(scenario21, linkToZoneResolver, allSightings);
        return scenario21;
	}

    public Map<Id, List<Sighting>> getSightingsPerPerson() {
        final Map<Id, List<Sighting>> allSightings = new HashMap<Id, List<Sighting>>();
        for (Sighting sighting : callProcess.getSightings()) {

            List<Sighting> sightingsOfPerson = allSightings.get(sighting.getAgentId());
            if (sightingsOfPerson == null) {
                sightingsOfPerson = new ArrayList<Sighting>();
                allSightings.put(sighting.getAgentId(), sightingsOfPerson);

            }
            sightingsOfPerson.add(sighting);
        }
        return allSightings;
    }


    public void close() {
		ticker.finish();
		callProcess.dump();
	}

	static double compareEMD(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
		Network network = scenario.getNetwork();
		Signature signature1 = makeSignature(network, groundTruthVolumes);
		Signature signature2 = makeSignature(network, cdrVolumes);
        return JFastEMD.distance(signature1, signature2, -1.0);
	}

	static double compareTimebins(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
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


    static double compareAllDay(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
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

	static double compareEMDMassPerLink(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
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

	public static Counts volumesToCounts(Network network, VolumesAnalyzer volumesAnalyzer) {
		Counts counts = new Counts();
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

    @Inject
	public CompareMain(Scenario scenario, EventsManager events, CallBehavior callingBehavior, LinkToZoneResolver linkToZoneResolver) {
		super();
		this.scenario = scenario;

		Map<Id<Person>, Id<Zone>> initialPersonInZone = new HashMap<>();


		// final Zones cellularCoverage = SyntheticCellTowerDistribution.naive(scenario.getNetwork());

        this.linkToZoneResolver = linkToZoneResolver;

		// linkToZoneResolver = new CellularCoverageLinkToZoneResolver(cellularCoverage, scenario.getNetwork());

		for (Person p : scenario.getPopulation().getPersons().values()) {
			Id<Link> linkId = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getLinkId();
			System.out.println(linkId);
			initialPersonInZone.put(p.getId(), this.linkToZoneResolver.resolveLinkToZone(linkId));
		}

		ticker = new CallProcessTicker();
		events.addHandler(ticker);


		ZoneTracker zoneTracker = new ZoneTracker(events, this.linkToZoneResolver, initialPersonInZone);
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
            for (int aVolumesForLink : volumesForLink) {
                if (aVolumesForLink != 0) {
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

	public static VolumesAnalyzer runWithTwoPlansAndCadyts(String outputDirectory, Network network, final LinkToZoneResolver linkToZoneResolver, Sightings allSightings, Counts counts) {
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
        config.controler().setOutputDirectory(outputDirectory);
		config.controler().setLastIteration(10);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);

		StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
		stratSets.setModuleName("ccc") ;
		stratSets.setProbability(1.) ;
		config.strategy().addStrategySettings(stratSets) ;


		final ScenarioImpl scenario2 = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario2.setNetwork(network);

		PopulationFromSightings.createPopulationWithTwoPlansEach(scenario2, linkToZoneResolver, allSightings);
//		PopulationFromSightings.preparePopulation(scenario2, linkToZoneResolver, allSightings);

		final CadytsContext context = new CadytsContext(config, counts) ;
		Controler controler = new Controler(scenario2);
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


    public static double calcCadytsScore(final CadytsContext context, Plan plan) {
		cadyts.demand.Plan<Link> currentPlanSteps = context.getPlansTranslator().getPlanSteps(plan);
        return context.getCalibrator().calcLinearPlanEffect(currentPlanSteps);
	}
	
	public static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Link link) {
		return getVolumesForLink(volumesAnalyzer1, link.getId());
	}

    public static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Id<Link> linkId) {
        int maxSlotIndex = (MAX_TIME / TIME_BIN_SIZE) + 1;
        int[] maybeVolumes = volumesAnalyzer1.getVolumesForLink(linkId);
        if(maybeVolumes == null) {
            return new int[maxSlotIndex + 1];
        }
        return maybeVolumes;
    }

}
