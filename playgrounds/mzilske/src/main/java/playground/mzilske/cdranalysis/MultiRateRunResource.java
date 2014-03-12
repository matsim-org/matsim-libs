package playground.mzilske.cdranalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.mzilske.cdr.CallBehavior;
import playground.mzilske.cdr.CompareMain;
import playground.mzilske.cdr.PowerPlans;

public class MultiRateRunResource {

	private String WD;

	private String regime;

	public MultiRateRunResource(String wd, String regime) {
		this.WD = wd;
		this.regime = regime;
	}

	final static int TIME_BIN_SIZE = 60*60;
	final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;

	public Collection<String> getRates() {
		final List<String> RATES = new ArrayList<String>();
		RATES.add(Integer.toString(0));
		RATES.add(Integer.toString(2));
		RATES.add(Integer.toString(5));
		RATES.add(Integer.toString(10));
		RATES.add(Integer.toString(20));
		RATES.add(Integer.toString(30));
		RATES.add(Integer.toString(40));
		RATES.add(Integer.toString(50));
		RATES.add(Integer.toString(100));
		RATES.add(Integer.toString(150));
		RATES.add("actevents");
		RATES.add("contbaseplans");
		return RATES;
	}

	public void rate(String string) {
		Scenario scenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
		if (string.equals("contbaseplans")) {
			runContinuedBasePlans(scenario);
		} else if (string.equals("actevents")) {
			runPhoneOnActivityStartEnd(scenario);
		} else {
			int rate = Integer.parseInt(string);
			runRate(scenario, rate);
		}
	}
	
	public void allRates() {
		for (String rate : getRates()) {
			rate(rate);
		}
	}

	private Config phoneConfigCongested() {
		Config config = ConfigUtils.createConfig();
		ActivityParams sightingParam = new ActivityParams("sighting");
		sightingParam.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(sightingParam);
		config.planCalcScore().setTraveling_utils_hr(-6);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(20);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(0.02);
		tmp.setStorageCapFactor(0.06);
		tmp.setRemoveStuckVehicles(false);
		tmp.setStuckTime(10.0);
		{
			StrategySettings stratSets = new StrategySettings(new IdImpl(1));
			stratSets.setModuleName("ChangeExpBeta");
			stratSets.setProbability(0.7);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings(new IdImpl(2));
			stratSets.setModuleName("ReRoute");
			stratSets.setProbability(0.3);
			config.strategy().addStrategySettings(stratSets);
		}
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		return config;
	}

	private static Config phoneConfigUncongested() {
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
		config.controler().setLastIteration(0);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(100);
		tmp.setStorageCapFactor(100);
		tmp.setRemoveStuckVehicles(false);
		tmp.setStuckTime(10.0);
		{
			StrategySettings stratSets = new StrategySettings(new IdImpl(1));
			stratSets.setModuleName("ChangeExpBeta");
			stratSets.setProbability(0.7);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings(new IdImpl(2));
			stratSets.setModuleName("ReRoute");
			stratSets.setProbability(0.3);
			config.strategy().addStrategySettings(stratSets);
		}
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		return config;
	}

	private void runPhoneOnActivityStartEnd(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		CompareMain compareMain = new CompareMain(scenario, events, new CallBehavior() {

			@Override
			public boolean makeACall(ActivityEndEvent event) {
				return true;
			}

			@Override
			public boolean makeACall(ActivityStartEvent event) {
				return true;
			}

			@Override
			public boolean makeACall(Id id, double time) {
				return false;
			}

			@Override
			public boolean makeACallAtMorningAndNight() {
				return true;
			}

		});
		new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());

		Config config = phoneConfig();
		config.controler().setOutputDirectory(WD + "/rates/actevents");
		compareMain.runOnceWithSimplePlans(config);
	}

	private void runRate(Scenario baseScenario, final int dailyRate) {
		EventsManager events = EventsUtils.createEventsManager();
		CompareMain compareMain = new CompareMain(baseScenario, events, new CallBehavior() {

			@Override
			public boolean makeACall(ActivityEndEvent event) {
				return false;
			}

			@Override
			public boolean makeACall(ActivityStartEvent event) {
				return false;
			}

			@Override
			public boolean makeACall(Id id, double time) {
				double secondlyProbability = dailyRate / (double) (24*60*60);
				return Math.random() < secondlyProbability;
			}

			@Override
			public boolean makeACallAtMorningAndNight() {
				return false;
			}

		});
		new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());
		Config config = phoneConfig();
		config.controler().setOutputDirectory(WD + "/rates/" + dailyRate);
		compareMain.runOnceWithSimplePlans(config);
	}

	private Config phoneConfig() {
		if (regime.equals("congested")) {
			return phoneConfigCongested();
		} else if (regime.equals("uncongested")) {
			return phoneConfigUncongested();
		}
		throw new RuntimeException("Unknown regime");
	}

	private void runContinuedBasePlans(Scenario baseScenario) {
		Config config = phoneConfig();
		for (ActivityParams params : baseScenario.getConfig().planCalcScore().getActivityParams()) {
			ActivityParams zero = new ActivityParams(params.getType());
			zero.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(zero);
		}
		config.controler().setOutputDirectory(WD + "/rates/contbaseplans");

		Scenario scenario = ScenarioUtils.createScenario(config);
		((ScenarioImpl) scenario).setNetwork(baseScenario.getNetwork());

		for (Person basePerson : baseScenario.getPopulation().getPersons().values()) {
			Person person = scenario.getPopulation().getFactory().createPerson(basePerson.getId());
			PlanImpl planImpl = (PlanImpl) scenario.getPopulation().getFactory().createPlan();
			planImpl.copyFrom(basePerson.getSelectedPlan());
			person.addPlan(planImpl);
			scenario.getPopulation().addPerson(person);
		}

		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();
	}

	public void distances() {
		File file = new File(WD + "/distances.txt");
		Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();

		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer baseVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
		events.addHandler(baseVolumes);
		new MatsimEventsReader(events).readFile(getBaseRun().getLastIteration().getEventsFileName());

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			pw.printf("callrate\troutesum\tvolumesum\tvolumesumdiff\n");
			for (String rate : getRates()) {
				final IterationResource lastIteration = getRateRun(rate).getLastIteration();
				Scenario scenario = lastIteration.getExperiencedPlansAndNetwork();


				final Map<Id, Double> distancePerPerson1 = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork());
				final Map<Id, Double> distancePerPerson = distancePerPerson1;
				double km = 0.0;
				for (double distance : distancePerPerson.values()) {
					km += distance;
				}

				EventsManager events1 = EventsUtils.createEventsManager();
				VolumesAnalyzer volumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
				events1.addHandler(volumes);
				new MatsimEventsReader(events1).readFile(lastIteration.getEventsFileName());

				double baseSum = PowerPlans.drivenKilometersWholeDay(baseScenario, baseVolumes);
				double sum = PowerPlans.drivenKilometersWholeDay(baseScenario, volumes);

				pw.printf("%s\t%f\t%f\t%f\n", rate, km, sum, baseSum - sum);
				pw.flush();
			}
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (pw != null) pw.close();
		}

	}

	private RunResource getRateRun(String rate) {
		return new RunResource(WD + "/rates/" + rate, null); 
	}

	public void personKilometers() {
		final File file = new File(WD + "/person-kilometers.txt");
		Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			final Map<Id, Double> distancePerPersonBase = PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork());
			pw.printf("person\tkilometers-base\tvariable\tvalue\tregime\n");
			for (String rate : getRates()) {
				Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				new MatsimPopulationReader(scenario).readFile(WD + "/rates/" + rate + "/ITERS/it.20/20.experienced_plans.xml.gz");
				final Map<Id, Double> distancePerPerson = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork());
				for (Person person : baseScenario.getPopulation().getPersons().values()) {
					pw.printf("%s\t%f\t%s\t%f\t%s\n", 
							person.getId().toString(), 
							zeroForNull(distancePerPersonBase.get(person.getId())),
							rate,
							zeroForNull(distancePerPerson.get(person.getId())),
							regime);
				}
			}
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (pw != null) pw.close();
		}

	}

	private RunResource getBaseRun() {
		return new RunResource(WD + "/output-berlin", "2kW.15");
	}

	public void permutations() {
		File file = new File(WD + "/permutations.txt");
		Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
		PowerPlans.writePermutations(baseScenario, file);
	}

	public void durationsSimulated() {
		File file = new File(WD + "/durations-simulated.txt");
		Scenario baseScenario = getBaseRun().getLastIteration().getExperiencedPlansAndNetwork();
		PowerPlans.writeActivityDurations(baseScenario, file);
	}

	private static Double zeroForNull(Double maybeDouble) {
		if (maybeDouble == null) {
			return 0.0;
		}
		return maybeDouble;
	}

}
