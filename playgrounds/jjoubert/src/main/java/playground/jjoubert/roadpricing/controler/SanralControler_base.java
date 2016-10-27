package playground.jjoubert.roadpricing.controler;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.jjoubert.Utilities.matsim2urbansim.controler.MyBasicConfig;

public class SanralControler_base {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String root = "/home/jwjoubert/runs/sanral_10pctFreight/data/";
		// Parse the necessary arguments.
		
		// Set up the basic config.
		MyBasicConfig mbc = new MyBasicConfig();
		Config config = mbc.getConfig();
		
		/*
		 * Customise the config file.
		 */
		// Global.
		config.global().setCoordinateSystem("WGS84_UTM35S");
		config.global().setNumberOfThreads(15);
		// Simulation.
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setFlowCapFactor(0.1);
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setStorageCapFactor(0.28);
//		((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).setSnapshotPeriod(900);
		Logger.getLogger("dummy").fatal("SimulationConfigGroup is no longer there.  Since `simulation' has been gone for some time now, "
				+ "I cannot see how the above may have worked so I am not fixing it.  kai, jul'15");
		System.exit(-1);

		// Network.
		config.network().setInputFile(root + "network/gautengNetwork_CleanV0.xml.gz");
		config.network();
		// Plans.
		config.plans().setInputFile(root + "plans/car-commercial_plans_2009_10pct.xml.gz");
		// Counts.
		config.counts().setInputFile(root + "counts/2007/Counts_Wednesday_Total.xml.gz");
		config.counts().setCountsScaleFactor(10);
		config.counts().setOutputFormat("kml");
		
		// Add minor and major activity types.
		ActivityParams minor = new ActivityParams("minor");
		minor.setPriority(1.0);
		minor.setMinimalDuration(600.0);	// 00:10:00
		minor.setTypicalDuration(1800.0);	// 00:30:00
		minor.setOpeningTime(0.0);			// 00:00:00
		minor.setLatestStartTime(86399.0);	// 23:59:59
		minor.setClosingTime(86399.0);		// 23:59:59
		config.planCalcScore().addActivityParams(minor);
		
		ActivityParams major = new ActivityParams("major");
		major.setPriority(1.0);
		major.setMinimalDuration(0.0);		// 00:00:00
		major.setTypicalDuration(10800.0);  // 03:00:00
		config.planCalcScore().addActivityParams(major);	
		

		/*
		 * Add the multi-thread queue simulation.
		 */
//		config.getQSimConfigGroup().setNumberOfThreads(5);
//		config.getQSimConfigGroup().setFlowCapFactor(0.1);
//		config.getQSimConfigGroup().setStorageCapFactor(0.28);

		MatsimServices c = new Controler(config);
        c.getConfig().controler().setCreateGraphs(true);
        c.getConfig().controler().setWriteEventsInterval(20);
		c.getConfig().controler().setOverwriteFileSetting(
				false ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );


		config.checkConsistency();
		new ConfigWriter(config).write("./output/Sanral_config.xml");
		
	}

}
