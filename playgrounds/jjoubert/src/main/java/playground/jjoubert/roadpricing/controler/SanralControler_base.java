package playground.jjoubert.roadpricing.controler;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

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
		config.simulation().setFlowCapFactor(0.1);
		config.simulation().setStorageCapFactor(0.28);
		config.simulation().setSnapshotPeriod(900);
		// Network.
		config.network().setInputFile(root + "network/gautengNetwork_CleanV0.xml.gz");
		config.network();
		// Plans.
		config.plans().setInputFile(root + "plans/car-commercial_plans_2009_10pct.xml.gz");
		// Counts.
		config.counts().setCountsFileName(root + "counts/2007/Counts_Wednesday_Total.xml.gz");
		config.counts().setCountsScaleFactor(10);
		config.counts().setOutputFormat("kml");
		

		/*
		 * Add the multi-thread queue simulation.
		 */
		config.getQSimConfigGroup().setNumberOfThreads(5);
		config.getQSimConfigGroup().setFlowCapFactor(0.1);
		config.getQSimConfigGroup().setStorageCapFactor(0.28);

		Controler c = new Controler(config);
		c.setCreateGraphs(true);
		c.setWriteEventsInterval(20);
		c.setOverwriteFiles(false);
		
		config.checkConsistency();
		c.run();
	}

}
