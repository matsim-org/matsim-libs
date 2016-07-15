package playground.santiago.utils;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.pt.config.TransitConfigGroup;

public class ChangeConfigToCluster {
	
	final static String pathForCluster = "/net/ils4/lcamus/runs-svn/santiago/basecase1/";
	final static String randomizedConfigFile = "../../../runs-svn/santiago/basecase1/input/new-input/";
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.loadConfig(randomizedConfigFile + "randomized_expanded_config_final.xml");

		ControlerConfigGroup cc = config.controler();
		cc.setOutputDirectory(pathForCluster + "output/" );

		CountsConfigGroup counts = config.counts();
		counts.setCountsFileName(pathForCluster + "input/counts_merged_VEH_C01.xml" );

		NetworkConfigGroup net = config.network();
		net.setInputFile(pathForCluster + "input/network_merged_cl.xml.gz" );
		
		PlansConfigGroup plans = config.plans();
		plans.setInputPersonAttributeFile(pathForCluster + "input/agentAttributes.xml" );
		plans.setInputFile(pathForCluster + "input/new-input/randomized_expanded_plans_final.xml.gz" );
		
		TransitConfigGroup transit = config.transit();
		transit.setTransitScheduleFile(pathForCluster + "input/transitschedule_simplified.xml" );
		transit.setVehiclesFile(pathForCluster + "input/transitvehicles.xml" );
		
		new ConfigWriter(config).write(randomizedConfigFile + "cluster_randomized_expanded_config_final.xml");
		
		
		
		
	}

}
