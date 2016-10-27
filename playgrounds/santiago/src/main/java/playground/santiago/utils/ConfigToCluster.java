package playground.santiago.utils;

import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.pt.config.TransitConfigGroup;

public class ConfigToCluster {
	
	final static String pathForCluster = "/net/ils4/lcamus/runs-svn/santiago/BASE10/";
	
	final static String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";
	final static String runsExpandedDir = "../../../runs-svn/santiago/BASE10/";
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.loadConfig(svnWorkingDir + "inputForMATSim/randomized_expanded_config.xml");

		ControlerConfigGroup cc = config.controler();
		cc.setOutputDirectory(pathForCluster + "output/" );

		CountsConfigGroup counts = config.counts();
		counts.setInputFile(pathForCluster + "input/counts_merged_VEH_C01.xml" );

		NetworkConfigGroup net = config.network();
		net.setInputFile(pathForCluster + "input/network_merged_cl.xml.gz" );
		
		PlansConfigGroup plans = config.plans();
		plans.setInputPersonAttributeFile(pathForCluster + "input/expandedAgentAttributes.xml" );
		plans.setInputFile(pathForCluster + "input/randomized_expanded_plans.xml.gz" );
		
		TransitConfigGroup transit = config.transit();
		transit.setTransitScheduleFile(pathForCluster + "input/transitschedule_simplified.xml" );
		transit.setVehiclesFile(pathForCluster + "input/transitvehicles.xml" );
		
		File file = new File(runsExpandedDir+"input/");
		if(!file.exists())	file.mkdirs();
		
		new ConfigWriter(config).write(runsExpandedDir + "input/cluster_randomized_expanded_config.xml");
		
		
		
		
	}

}
