package playground.dhosse.gap.scenario.config;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.config.TransitConfigGroup;

import playground.dhosse.gap.Global;

public class ConfigCreator {
	
	public static void configureConfig(Config config){
		
//		TODO configure config groups
		configureGlobalConfigGroup(config.global());
		configureParallelEventsHandlingConfigGroup(config.parallelEventHandling());
		configureControlerConfigGroup(config.controler());
		configurePlanCalcScoreConfigGroup(config.planCalcScore());
		configureStrategyConfigGroup(config.strategy());
		configureTransitConfigGroup(config.transit());
		configureFacilitiesConfigGroup(config.facilities());
		configurePlansConfigGroup(config.plans());
		configureNetworkConfigGroup(config.network());
		
	}
	
	public static void configureQSimAndCountsConfigGroups(Config config){
		
		configureQSimConfigGroup(config.qsim());
		configureCountsConfigGroup(config.counts());
		
	}
	
	private static void configureNetworkConfigGroup(NetworkConfigGroup network){
		
		network.setInputFile(Global.matsimInputDir + "Netzwerk/network_merged.xml.gz");
		
	}
	
	private static void configurePlansConfigGroup(PlansConfigGroup plans){
		
		plans.setInputFile(Global.matsimInputDir + "Pläne/plans_mid.xml.gz");
		
	}
	
	private static void configureFacilitiesConfigGroup(FacilitiesConfigGroup facilities){
		
//		facilities.setInputFile(GAPMain.matsimInputDir + "facilities/facilities.xml");
//		facilities.setInputFacilitiesAttributesFile(GAPMain.matsimInputDir + "facilities/facilityAttributes.xml");
		
	}
	
	private static void configureTransitConfigGroup(TransitConfigGroup transit){
		transit.setUseTransit(true);
		transit.setTransitScheduleFile(Global.matsimInputDir + "transit/scheduleComplete.xml");
		transit.setVehiclesFile(Global.matsimInputDir + "transit/transitVehicles.xml");
	}
	
	private static void configureGlobalConfigGroup(GlobalConfigGroup global){
		
		global.setCoordinateSystem(Global.toCrs);
		global.setNumberOfThreads(2);
		
	}
	
	private static void configureParallelEventsHandlingConfigGroup(ParallelEventHandlingConfigGroup peh){
		
		peh.setNumberOfThreads(2);
		
	}
	
	private static void configureControlerConfigGroup(ControlerConfigGroup controler){
		
		controler.setFirstIteration(0);
		controler.setLastIteration(0);
		controler.setOutputDirectory(Global.matsimOutputDir);
		controler.setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		controler.setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
		
	}
	
	private static void configurePlanCalcScoreConfigGroup(PlanCalcScoreConfigGroup pcs){
		
		pcs.setBrainExpBeta(1);
		pcs.setEarlyDeparture_utils_hr(-0);
		pcs.setFractionOfIterationsToStartScoreMSA(0.8);
		pcs.setLateArrival_utils_hr(-12);
		pcs.setLearningRate(1);
		pcs.setMarginalUtilityOfMoney(1);
		pcs.setPathSizeLogitBeta(1);
		pcs.setUsingOldScoringBelowZeroUtilityDuration(false);

		final double traveling = -6.0;
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		pcs.setPerforming_utils_hr(6.0);
		
	}
	
	private static void configureStrategyConfigGroup(StrategyConfigGroup strategy){
		
		strategy.setFractionOfIterationsToDisableInnovation(0.8);
		strategy.setMaxAgentPlanMemorySize(5);
		
		StrategySettings changeExp = new StrategySettings();
		changeExp.setDisableAfter(-1);
		changeExp.setStrategyName("ChangeExpBeta");
		changeExp.setWeight(0.7);
		strategy.addStrategySettings(changeExp);
		
		StrategySettings reroute = new StrategySettings();
		reroute.setStrategyName("ReRoute");
		reroute.setWeight(0.3);
		strategy.addStrategySettings(reroute);
		
	}
	
	private static void configureCountsConfigGroup(CountsConfigGroup counts){
		
		counts.setAnalyzedModes(TransportMode.car);
		counts.setAverageCountsOverIterations(1);
		counts.setCountsFileName(Global.matsimInputDir + "Counts/counts.xml");
		counts.setCountsScaleFactor(Global.N/Global.getN());
		counts.setDistanceFilter(null);
		counts.setDistanceFilterCenterNode(null);
		counts.setFilterModes(false);
		counts.setOutputFormat("all");
		counts.setWriteCountsInterval(10);
		
	}
	
	private static void configureQSimConfigGroup(QSimConfigGroup qsim){
	
		qsim.setEndTime(Time.UNDEFINED_TIME);
		qsim.setFlowCapFactor(Global.getN()/Global.N);
		qsim.setInsertingWaitingVehiclesBeforeDrivingVehicles(false);
		qsim.setLinkDynamics(LinkDynamics.FIFO.name());
		qsim.setLinkWidth(30L);
		Set<String> mainModes = new HashSet<>();
		mainModes.add(TransportMode.car);
		qsim.setMainModes(mainModes);
		qsim.setNodeOffset(0);
		qsim.setNumberOfThreads(2);
		qsim.setRemoveStuckVehicles(false);
		qsim.setSimStarttimeInterpretation(StarttimeInterpretation.maxOfStarttimeAndEarliestActivityEnd);
		qsim.setSnapshotPeriod(0);
		qsim.setSnapshotStyle(SnapshotStyle.queue);
		qsim.setStartTime(Time.UNDEFINED_TIME);
		qsim.setStorageCapFactor(Global.getN()/Global.N);
		qsim.setStuckTime(10);
		qsim.setTimeStepSize(1);
		qsim.setTrafficDynamics(TrafficDynamics.queue);
		qsim.setUseLanes(false);
		qsim.setUsePersonIdForMissingVehicleId(true);
		qsim.setUsingFastCapacityUpdate(false);
		qsim.setUsingThreadpool(false);
		qsim.setVehicleBehavior(VehicleBehavior.teleport);
		qsim.setVehiclesSource(VehiclesSource.defaultVehicle);
		
	}

}
