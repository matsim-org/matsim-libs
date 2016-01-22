package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

public class PatnaUrbanConfigGenerator {

	private Config config;

	public static void main(String[] args) {
		String outputConfig = "";
		PatnaUrbanConfigGenerator configFile = new PatnaUrbanConfigGenerator();
		configFile.createBasicConfigSettings();
		new ConfigWriter(configFile.getPatnaConfig()).write(outputConfig);
	}

	/**
	 * This config do not have locations of inputs files (network, plans, counts etc).
	 */
	public  void createBasicConfigSettings () {
		config = ConfigUtils.createConfig();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(200);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		
		config.counts().setWriteCountsInterval(0);
		config.counts().setCountsScaleFactor(94.52);
		//ZZ_TODO : there is something about multipleModes in counts. I could not see any effect of it.
		
		config.qsim().setFlowCapFactor(0.011); //1.06% sample
		config.qsim().setStorageCapFactor(0.033);
		config.qsim().setEndTime(36*3600);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.toString());
		config.qsim().setMainModes(PatnaUtils.URBAN_MAIN_MODES);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		
		config.setParam("TimeAllocationMutator", "mutationAffectsDuration", "false");
		config.setParam("TimeAllocationMutator", "mutationRange", "7200.0");

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.85);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);

		StrategySettings timeAllocationMutator	= new StrategySettings();
		timeAllocationMutator.setStrategyName("TimeAllocationMutator");
		timeAllocationMutator.setWeight(0.05);

		config.strategy().addStrategySettings(expChangeBeta);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(timeAllocationMutator);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().setWritingOutputEvents(true);

		ActivityParams workAct = new ActivityParams("work");
		workAct.setTypicalDuration(8*3600);
		config.planCalcScore().addActivityParams(workAct);

		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(12*3600);
		config.planCalcScore().addActivityParams(homeAct);

		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0);
		config.planCalcScore().setPerforming_utils_hr(6.0);

		ModeParams car = new ModeParams("car");
		car.setConstant(-3.30);
		car.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(car);

		ModeParams bike = new ModeParams("bike");
		bike.setConstant(0.0);
		bike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(bike);

		ModeParams motorbike = new ModeParams("motorbike");
		motorbike.setConstant(-2.20);
		motorbike.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(motorbike);

		ModeParams pt = new ModeParams("pt");
		pt.setConstant(-3.40);
		pt.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(pt);

		ModeParams walk = new ModeParams("walk");
		walk.setConstant(0.0);
		walk.setMarginalUtilityOfTraveling(0.0);
		config.planCalcScore().addModeParams(walk);

		config.plansCalcRoute().setNetworkModes(PatnaUtils.URBAN_MAIN_MODES);

		{
			ModeRoutingParams mrp = new ModeRoutingParams("walk");
			mrp.setTeleportedModeSpeed(4./3.6);
			mrp.setBeelineDistanceFactor(1.1);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}

		{
			ModeRoutingParams mrp = new ModeRoutingParams("pt");
			mrp.setTeleportedModeSpeed(20./3.6);
			mrp.setBeelineDistanceFactor(1.5);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
	}

	public Config getPatnaConfig(){
		return this.config;
	}
}
