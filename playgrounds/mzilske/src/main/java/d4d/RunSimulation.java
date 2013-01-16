package d4d;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonAlgorithm;

public class RunSimulation {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.addQSimConfigGroup(new QSimConfigGroup());

		config.controler().setLastIteration(20);
		config.controler().setMobsim("jdeqsim");
		// config.controler().setMobsim("DoNothing");
		config.global().setCoordinateSystem("EPSG:3395");
		config.global().setNumberOfThreads(8);
		config.otfVis().setShowTeleportedAgents(false);
		config.controler().setWriteSnapshotsInterval(5);
		config.getQSimConfigGroup().setStorageCapFactor(0.01);
		config.getQSimConfigGroup().setFlowCapFactor(0.01);
		config.getQSimConfigGroup().setSnapshotStyle(QSimConfigGroup.SNAPSHOT_AS_QUEUE);
		config.getQSimConfigGroup().setRemoveStuckVehicles(false);
		config.getQSimConfigGroup().setNumberOfThreads(8);
		config.getQSimConfigGroup().setEndTime(27*60*60);
		config.plansCalcRoute().setTeleportedModeSpeed("other", 1.38889); // 5 km/h beeline
		config.plansCalcRoute().setNetworkModes(Arrays.asList("car"));
		config.controler().setWriteEventsInterval(10);
		ActivityParams sighting = new ActivityParams("sighting");
		// sighting.setOpeningTime(0.0);
		// sighting.setClosingTime(0.0);
		sighting.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sighting);
		config.planCalcScore().setTraveling_utils_hr(0);
		config.planCalcScore().setConstantCar(0);
		config.planCalcScore().setMonetaryDistanceCostRateCar(0);
		// config.planCalcScore().setWriteExperiencedPlans(true);
		config.setParam("JDEQSim", "flowCapacityFactor", "0.01");
		config.setParam("JDEQSim", "storageCapacityFactor", "0.01");
		double endTime= 60*60*32;
		config.setParam("JDEQSim", "endTime", Double.toString(endTime));
		
		config.setParam("changeLegMode", "modes", "car,other");
		config.setParam("changeLegMode", "ignoreCarAvailability", "true");
		
		
		StrategySettings changeExp = new StrategySettings(new IdImpl(1));
		changeExp.setModuleName("ChangeExpBeta");
		changeExp.setProbability(0.6);
		StrategySettings reRoute = new StrategySettings(new IdImpl(2));
		reRoute.setModuleName("ReRoute");
		// reRoute.setModuleName("Duplicate");
		reRoute.setProbability(0.2);
		reRoute.setDisableAfter(450);
		StrategySettings changeMode = new StrategySettings(new IdImpl(3));
		changeMode.setModuleName("ChangeLegMode");
		// changeMode.setModuleName("Duplicate");
		changeMode.setProbability(0.2);
		changeMode.setDisableAfter(450);
		config.strategy().addStrategySettings(changeExp);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(changeMode);
		config.strategy().setMaxAgentPlanMemorySize(3);
		
		
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("/Users/zilske/d4d/output/network.xml");
		AltPopulationReaderMatsimV5 altPopulationReaderMatsimV5 = new AltPopulationReaderMatsimV5(scenario);
		//	altPopulationReaderMatsimV5.readFile("/Users/zilske/d4d/output/population.xml");
		altPopulationReaderMatsimV5.readFile("/Users/zilske/d4d/output/population.xml");
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				PlanUtils.insertLinkIdsIntoGenericRoutes(person.getSelectedPlan());
			}

		});

		Controler controler = new Controler(scenario);
		controler.addPlanStrategyFactory("Duplicate", new DuplicatePlanStrategyFactory());
		controler.setOverwriteFiles(true);
		controler.addMobsimFactory("DoNothing", new MobsimFactory() {

			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				return new Mobsim() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						
					}
					
				};
			}
			
		});
		controler.run();
	}

}
