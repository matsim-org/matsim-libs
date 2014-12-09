package playground.agarwalamit.patnaIndia;

import java.util.Arrays;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

import playground.agarwalamit.mixedTraffic.VehiclesConfigGroup;

public class WriteConfig {
	/**
	 * @param args
	 */
	private final String outputDir = MyFirstControler.outputDir;
	public static void main(String[] args) {
		WriteConfig configFile = new WriteConfig();
		configFile.configRun();
	}

	public  void configRun () {
		Config config = ConfigUtils.createConfig();
		Collection <String> mainModes = Arrays.asList("car","motorbike","bike");

		config.plans().setInputFile(outputDir+"/plans.xml");
		//		config.plans().setInputFile("./patnaOutput/modeChoice/selectedPlansOnly/plans.xml");
		config.network().setInputFile(outputDir+"/network.xml");
		config.counts().setCountsFileName("../../patnaIndiaSim/input/counts/countsCarMotorbikeBike.xml");
		config.counts().setOutputFormat("all");
		config.counts().setWriteCountsInterval(100);
		config.counts().setCountsScaleFactor(94.52); 
		config.controler().setOutputDirectory(outputDir);

		//===
//		VehiclesConfigGroup vehiclesCnfGrp = new VehiclesConfigGroup();
//		vehiclesCnfGrp.setInputFile(outputDir+"/vehiclesPatna.xml");
//		vehiclesCnfGrp.setMainModes(mainModes);
//		config.addModule(vehiclesCnfGrp);
		//===


		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		config.controler().setMobsim("qsim");
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteSnapshotsInterval(100);	

		config.controler().setSnapshotFormat(Arrays.asList("otfvis"));

		//config.scenario().setUseKnowledge(false);

		//QSim is now default mobsim now therefore, it can be added directly as shown below now. 9 oct 2013

		config.qsim().setFlowCapFactor(0.011);		//1.06% sample
		config.qsim().setStorageCapFactor(0.033);
		config.qsim().setRemoveStuckVehicles(false); 
		config.qsim().setSnapshotStyle("equiDist");
		config.qsim().setSnapshotPeriod(5*60);
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(36*3600);
		config.qsim().setStuckTime(10);
		config.qsim().setTimeStepSize(1);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.toString());


		config.qsim().setMainModes(mainModes);
		config.qsim().setTrafficDynamics("queue");

		StrategySettings expChangeBeta = new StrategySettings(Id.create("1",StrategySettings.class));
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.9);

		StrategySettings reRoute = new StrategySettings(Id.create("2",StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);

//		StrategySettings modeChoice = new StrategySettings(Id.create("4",StrategySettings.class));
//		modeChoice.setModuleName("ChangeLegMode");
//		modeChoice.setProbability(0.05);

		StrategySettings timeAllocationMutator	= new StrategySettings(Id.create("3",StrategySettings.class));
		timeAllocationMutator.setStrategyName("TimeAllocationMutator");
		timeAllocationMutator.setWeight(0.05);

//		config.setParam("changeLegMode", "modes", "car,bike,motorbike,pt,walk");

		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(expChangeBeta);
		config.strategy().addStrategySettings(reRoute);
//		config.strategy().addStrategySettings(modeChoice);
		config.strategy().addStrategySettings(timeAllocationMutator);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		//vsp default
		config.setParam( "TimeAllocationMutator",  "mutationRange",  "7200"); 
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		//vsp default

		ActivityParams workAct = new ActivityParams("work");
		workAct.setTypicalDuration(8*3600);
		config.planCalcScore().addActivityParams(workAct);

		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(12*3600);
		config.planCalcScore().addActivityParams(homeAct);


		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);// changed to 0 from (-2) earlier

		config.planCalcScore().setPerforming_utils_hr(6.0);
		config.planCalcScore().setTraveling_utils_hr(0);
		config.planCalcScore().setTravelingBike_utils_hr(0);
		config.planCalcScore().setTravelingOther_utils_hr(0);
		config.planCalcScore().setTravelingPt_utils_hr(0);
		config.planCalcScore().setTravelingWalk_utils_hr(0);


//		config.planCalcScore().setConstantCar(-3.50);
//		config.planCalcScore().setConstantOther(-2.2);
//		config.planCalcScore().setConstantBike(0);
//		config.planCalcScore().setConstantPt(-3.4);
//		config.planCalcScore().setConstantWalk(-0.0);

		//config.planCalcScore().getOrCreateModeParams("bike").setMarginalUtilityOfDistance(-0.01);

		config.plansCalcRoute().setNetworkModes(mainModes);
		config.plansCalcRoute().setBeelineDistanceFactor(1.0);
		/*
		 * Beeline distnace factor is set to one so now the travel time will be calculated by given speed, 
		 * assuming in real scenario how much time it can take
		 * For example for walk trips 5 - 6 kph is normal speed but in that case travel time will be
		 * total beeline distance/ speed; which will not be realistic, so let's make it to 4 kph
		 * similarly for bus also normal speed can be taken as 40 kph so teleported mode speed should be set to 20kph
		 * Travel distance is taken care of in event handlers by taking a factor of 1.1 and 1.5 for walk and pt respectively
		 */
		config.plansCalcRoute().setTeleportedModeSpeed("walk", 4/3.6); 
		config.plansCalcRoute().setTeleportedModeSpeed("pt", 20/3.6);
		//		config.plansCalcRoute().setTeleportedModeSpeed("motorbike", 20/3.6);
		//		config.plansCalcRoute().setTeleportedModeSpeed("car", 20/3.6);
		//		config.plansCalcRoute().setTeleportedModeSpeed("bike",10/3.6);

		new ConfigWriter(config).write(outputDir+"/configPatnaSeepage_true.xml");
	}
}
