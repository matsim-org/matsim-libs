package playground.santiago.utils;

import java.io.File;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.roadpricing.RoadPricingConfigGroup;

public class ConfigToBaseCases {

	
	final static String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/";	
	final static String localPersonalFolder = svnWorkingDir + "LeoCamus/";
	
	final static String baseCase1pct = "baseCase1pct";
	final static String baseCase10pct = "baseCase10pct";
	
	final static String runsClusterDir = "/net/ils4/lcamus/runs-svn/santiago/" + baseCase10pct + "/";
	final static String runsLocalDir = "../../../runs-svn/santiago/" + baseCase1pct + "/";	


	
	public static void main(String args[]){
		File file = new File(localPersonalFolder);
		if(!file.exists())	file.mkdirs();
		write10pctConfig();
		write1pctConfig();
		
	}
	
	private static void write10pctConfig(){
		
		Config config = ConfigUtils.loadConfig(svnWorkingDir + "randomized_expanded_config.xml");

		ControlerConfigGroup cc = config.controler();
		cc.setOutputDirectory(runsClusterDir + "output/" );

		CountsConfigGroup counts = config.counts();
		counts.setInputFile(runsClusterDir + "input/counts_merged_VEH_C01.xml" );

		NetworkConfigGroup net = config.network();
		net.setInputFile(runsClusterDir + "input/network_merged_cl.xml.gz" );
		
		PlansConfigGroup plans = config.plans();
		plans.setInputPersonAttributeFile(runsClusterDir + "input/expandedAgentAttributes.xml" );
		plans.setInputFile(runsClusterDir + "input/randomized_expanded_plans.xml.gz" );
		
		TransitConfigGroup transit = config.transit();
		transit.setTransitScheduleFile(runsClusterDir + "input/transitschedule_simplified.xml" );
		transit.setVehiclesFile(runsClusterDir + "input/transitvehicles.xml" );
		
		RoadPricingConfigGroup rpcg = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
		rpcg.setTollLinksFile(runsClusterDir + "input/gantries.xml");		
		
		/*Values from calibration - v2b*/
		PlanCalcScoreConfigGroup planCalc = config.planCalcScore();
		planCalc.getModes().get(TransportMode.car).setConstant((double) 0.8383);
		planCalc.getModes().get(TransportMode.pt).setConstant((double) -1.6764);
		planCalc.getModes().get(TransportMode.walk).setConstant((double) -0.2536);
		/**/
		
		new ConfigWriter(config).write(localPersonalFolder + "config_" +  baseCase10pct + ".xml" );
		
	}
	
	private static void write1pctConfig(){
		
		Config config = ConfigUtils.loadConfig(svnWorkingDir + "randomized_sampled_config.xml");

		ControlerConfigGroup cc = config.controler();
		cc.setOutputDirectory(runsLocalDir + "output/" );

		CountsConfigGroup counts = config.counts();
		counts.setInputFile(runsLocalDir + "input/counts_merged_VEH_C01.xml" );

		NetworkConfigGroup net = config.network();
		net.setInputFile(runsLocalDir + "input/network_merged_cl.xml.gz" );
		
		PlansConfigGroup plans = config.plans();
		plans.setInputPersonAttributeFile(runsLocalDir + "input/sampledAgentAttributes.xml" );
		plans.setInputFile(runsLocalDir + "input/randomized_sampled_plans.xml.gz" );
		
		TransitConfigGroup transit = config.transit();
		transit.setTransitScheduleFile(runsLocalDir + "input/transitschedule_simplified.xml" );
		transit.setVehiclesFile(runsLocalDir + "input/transitvehicles.xml" );
		
		RoadPricingConfigGroup rpcg = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
		rpcg.setTollLinksFile(runsLocalDir + "input/gantries.xml");
		
		/*Values from calibration - v2a*/
		PlanCalcScoreConfigGroup planCalc = config.planCalcScore();
		planCalc.getModes().get(TransportMode.car).setConstant((double) 1.2652);
		planCalc.getModes().get(TransportMode.pt).setConstant((double) -0.6953);
		planCalc.getModes().get(TransportMode.walk).setConstant((double) -1.1828);
		/**/
		
		new ConfigWriter(config).write(localPersonalFolder + "config_" + baseCase1pct + ".xml" );
	}
	
	
	
		
}
