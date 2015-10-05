package playground.mzilske.d4d;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonAlgorithm;

public class RunFreeSpeedSimulation {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory("./freespeed-output");
		config.controler().setMobsim(MobsimType.JDEQSim.toString());
		// config.controler().setMobsim("DoNothing");
		config.global().setCoordinateSystem("EPSG:3395");
		config.global().setNumberOfThreads(8);
		config.controler().setWriteSnapshotsInterval(5);
//		config.getQSimConfigGroup().setStorageCapFactor(100);
//		config.getQSimConfigGroup().setFlowCapFactor(100);
//		config.getQSimConfigGroup().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
//		config.getQSimConfigGroup().setRemoveStuckVehicles(false);
//		config.getQSimConfigGroup().setNumberOfThreads(8);
//		config.getQSimConfigGroup().setEndTime(27*60*60);
		config.plansCalcRoute().setTeleportedModeSpeed("other", 5.0 / 3.6); // 5 km/h beeline
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);
		config.plansCalcRoute().setNetworkModes(Arrays.asList("car"));
		config.controler().setWriteEventsInterval(10);
		ActivityParams sighting = new ActivityParams("sighting");
		// sighting.setOpeningTime(0.0);
		// sighting.setClosingTime(0.0);
		sighting.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sighting);
		config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling((double) 0);
		config.planCalcScore().getModes().get(TransportMode.car).setConstant((double) 0);
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate((double) 0);
		// config.planCalcScore().setWriteExperiencedPlans(true);
		config.setParam("JDEQSim", "flowCapacityFactor", "100");
		config.setParam("JDEQSim", "storageCapacityFactor", "100");
		double endTime= 60*60*32;
		config.setParam("JDEQSim", "endTime", Double.toString(endTime));
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("/Users/zilske/d4d/output/network.xml");
		AltPopulationReaderMatsimV5 altPopulationReaderMatsimV5 = new AltPopulationReaderMatsimV5(scenario);
		//	altPopulationReaderMatsimV5.readFile("/Users/zilske/d4d/output/population.xml");
		altPopulationReaderMatsimV5.readFile("/Users/zilske/d4d/output/population-capital-only.xml");
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
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}

}
