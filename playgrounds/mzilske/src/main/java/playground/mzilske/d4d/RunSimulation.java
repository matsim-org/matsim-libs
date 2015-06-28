package playground.mzilske.d4d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonAlgorithm;

public class RunSimulation {
	
	private static final class ModeShareListener implements StartupListener, IterationEndsListener, ShutdownListener {
		private final Scenario scenario;
		private BufferedWriter out;

		private ModeShareListener(Scenario scenario) {
			this.scenario = scenario;
		}

		@Override
		public void notifyShutdown(ShutdownEvent event) {
			try {
				this.out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			Map <String,Integer> plansPerMode = new HashMap<String,Integer>();
			plansPerMode.put("car",0);
			plansPerMode.put("other",0);
			plansPerMode.put("none",0);
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Plan plan = person.getSelectedPlan();
				String mode = getMode(plan);
				plansPerMode.put(mode, plansPerMode.get(mode)+1);
			}
			try {
				this.out.write(event.getIteration() + "\t" + plansPerMode.get("car") +"\t" + plansPerMode.get("other") +"\t"+ plansPerMode.get("none") +"\t\n");
				this.out.flush();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private String getMode(Plan plan) {
			if (plan.getPlanElements().size()<3) {
				return "none";
			} else {
				return ((Leg) (plan.getPlanElements().get(1))).getMode();
			}
		}

		@Override
		public void notifyStartup(StartupEvent event) {
			String filename = event.getControler().getControlerIO().getOutputFilename("modeshare.txt");
			this.out = IOUtils.getBufferedWriter(filename);
			try {
				this.out.write("ITERATION\tcar\tother\tnone\n");
				this.out.flush();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	public static void main(String[] args) {
		Config config = createConfig(D4DConsts.WORK_DIR + "/0620wo-capital-only-025freespeed-beginning-disutility-travel-qs", 4);
		run(config);
//		config = createConfig(D4DConsts.WORK_DIR + "/0213-capital-only-05freespeed-beginning-disutility-travel-qs", 2);
//		run(config);
//		config = createConfig("./0211-capital-only-0125freespeed-beginning-disutility-travel", 8);
//		run(config);
//		config = createConfig("./0211-capital-only-1freespeed-beginning-disutility-travel", 1);
//		run(config);
//		config = createConfig("./0211-capital-only-2freespeed-beginning-disutility-travel", 0.5);
//		run(config);
	}

	private static void run(Config config) {
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(D4DConsts.WORK_DIR + "network-simplified.xml");
		AltPopulationReaderMatsimV5 altPopulationReaderMatsimV5 = new AltPopulationReaderMatsimV5(scenario);
		//	altPopulationReaderMatsimV5.readFile("/Users/zilske/d4d/output/population.xml");
		altPopulationReaderMatsimV5.readFile(D4DConsts.WORK_DIR + "population-capital-only.xml");
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				PlanUtils.insertLinkIdsIntoGenericRoutes(person.getSelectedPlan());
			}

		});
		
//		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithm() {
//
//			@Override
//			public void run(Person person) {
//				Plan plan = person.getSelectedPlan();
//				for (int i = 0; i < plan.getPlanElements().size()-2; i++) {
//					PlanElement pe = plan.getPlanElements().get(i);
//					if (pe instanceof Activity) {
//						Activity activity = (Activity) pe;
//						Leg leg = (Leg) plan.getPlanElements().get(i+1);
//						Activity nextActivity = (Activity) plan.getPlanElements().get(i+2);
//						double earliest = activity.getEndTime();
//						double latest = nextActivity.getEndTime() - leg.getTravelTime();
//						activity.setEndTime(earliest + MatsimRandom.getRandom().nextDouble() * (latest - earliest));
//					}
//				}
//			}
//
//		});
		
		Controler controler = new Controler(scenario);
		controler.addControlerListener(new ModeShareListener(scenario));
		controler.run();
	}

	public static Config createConfig(String outputDirectory, double travelTimeFactor) {
		Config config = ConfigUtils.createConfig();
		
		config.planCalcScore().setWriteExperiencedPlans(true);
		
	//	config.controler().setLastIteration(180);
		config.controler().setLastIteration(1);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.controler().setOutputDirectory(outputDirectory);
		// config.controler().setMobsim("DoNothing");
		config.global().setCoordinateSystem("EPSG:3395");
		config.global().setNumberOfThreads(8);
		// config.controler().setWriteSnapshotsInterval(5);
		config.qsim().setStorageCapFactor(0.01);
		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setNumberOfThreads(2);
		config.qsim().setEndTime(30*60*60);
	
		
//		config.plansCalcRoute().setTeleportedModeSpeed("other", 30.0 / 3.6); //  km/h beeline
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);
		
		config.plansCalcRoute().setTeleportedModeFreespeedFactor("other", travelTimeFactor); 
		
		
		config.plansCalcRoute().setNetworkModes(Arrays.asList("car"));
		config.controler().setWriteEventsInterval(10);
		ActivityParams sighting = new ActivityParams("sighting");
		// sighting.setOpeningTime(0.0);
		// sighting.setClosingTime(0.0);
		sighting.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sighting);
		config.planCalcScore().setTraveling_utils_hr(-6);
		config.planCalcScore().setPerforming_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(-6);
		config.planCalcScore().setConstantCar(0);
		config.planCalcScore().setMonetaryDistanceCostRateCar(0);
		// config.planCalcScore().setWriteExperiencedPlans(true);
//		config.setParam("JDEQSim", "flowCapacityFactor", "0.01");
//		config.setParam("JDEQSim", "storageCapacityFactor", "0.05");
		double endTime= 60*60*32;
		// config.setParam("JDEQSim", "endTime", Double.toString(endTime));
		
		config.setParam("changeLegMode", "modes", "car,other");
		config.setParam("changeLegMode", "ignoreCarAvailability", "true");
		
		
		StrategySettings changeExp = new StrategySettings(Id.create(1, StrategySettings.class));
		changeExp.setStrategyName("ChangeExpBeta");
		changeExp.setWeight(0.8);
		StrategySettings reRoute = new StrategySettings(Id.create(2, StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		// reRoute.setModuleName("Duplicate");
		reRoute.setWeight(0.1);
		reRoute.setDisableAfter(150);
		StrategySettings changeMode = new StrategySettings(Id.create(3, StrategySettings.class));
		changeMode.setStrategyName("ChangeLegMode");
		// changeMode.setModuleName("Duplicate");
		changeMode.setWeight(0.1);
		changeMode.setDisableAfter(150);
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(changeExp);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(changeMode);
		return config;
	}

}
