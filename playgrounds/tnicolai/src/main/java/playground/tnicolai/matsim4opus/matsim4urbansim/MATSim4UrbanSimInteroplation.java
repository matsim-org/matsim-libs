package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;

public class MATSim4UrbanSimInteroplation extends MATSim4UrbanSim{

	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimInteroplation.class);
	
	// resolution
	private static int resolutionMeter = -1;
	private static int resolutionFeet = -1;
	// job sample (default 100%)
	private static double jobSample = 1.;

	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimInteroplation(String args[]){
		super(args);
		initResolutionAndJobSample();
	}
	
	private void initResolutionAndJobSample(){
		try{
			String params[] = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.TEST_PARAMETER_PARAM).split(",");
			
			for(int i = 0; i < params.length; i++){
				
				if(params[i].startsWith("resolution")){
					String s[] = params[i].split("=");
					MATSim4UrbanSimInteroplation.resolutionMeter = Integer.parseInt(s[1]);
					log.info("Detected resolution in meter: " + MATSim4UrbanSimInteroplation.resolutionMeter);
					MATSim4UrbanSimInteroplation.resolutionFeet = (int)(MATSim4UrbanSimInteroplation.resolutionMeter * Constants.METER_IN_FEET_CONVERSION_FACTOR);
					log.info("Converted resolution into feet (used for goolge maps output): " + MATSim4UrbanSimInteroplation.resolutionFeet);
				}
				else if(params[i].startsWith("jobsample")){
					String s[] = params[i].split("=");
					MATSim4UrbanSimInteroplation.jobSample = Double.parseDouble(s[1]);
					log.info("Detected job sample size: " + MATSim4UrbanSimInteroplation.jobSample);
				}	
			}
		}
		catch(Exception e){ System.exit(-1); }
	}

	void runMATSim(){
		
		log.info("Starting MATSim from Urbansim");
		int benchmarkID = this.benchmark.addMeasure("MATSim4UrbanSimInteroplation Run");
		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkImpl network = scenario.getNetwork();
		modifyNetwork(network);
		cleanNetwork(network);
		
		ReadFromUrbansimParcelModel readUrbanSimData = new ReadFromUrbansimParcelModel( Integer.parseInt( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.YEAR) ) );
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		readUrbanSimData.readFacilities(parcels, zones);
		// set population in scenario
		scenario.setPopulation( readUrbansimPersons(readUrbanSimData, parcels, network) );
		
		log.info("### DONE with demand generation from UrbanSim ###");
		
		// gather all workplaces, workplaces are aggregated with respect to their nearest Node
		JobClusterObject[] aggregatedWorkplaces = readUrbanSimData.getAggregatedWorkplaces(parcels, jobSample, network);
		
		// Running the controler
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		//controler.addControlerListener( new ERSAControlerListenerV2(aggregatedWorkplaces, resolutionFeet, resolutionMeter, this.benchmark) );
		controler.addControlerListener( new ERSAControlerListenerV3(aggregatedWorkplaces, resolutionMeter, benchmark));
		controler.addControlerListener( new MATSim4UrbanSimControlerListenerV3(zones, parcels, scenario));
		
		controler.run();
		// Controller done!
		
		if( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BACKUP_RUN_DATA_PARAM).equalsIgnoreCase("TRUE") ){ // tnicolai: Experimental, comment out for MATSim4UrbanSim release
			// saving results from current run
			saveRunOutputs();			
			cleanUrbanSimOutput();
		}
		
		this.benchmark.stoppMeasurement(benchmarkID);
		log.info("MATSim4UrbanSimInteroplation Run took " + this.benchmark.getDurationInSeconds(benchmarkID) + "seconds ("+ this.benchmark.getDurationInSeconds(benchmarkID) / 60. + " minutes)");
		// dumping benchmark results
		benchmark.dumpResults(Constants.MATSIM_4_OPUS_TEMP + "matsim4ersa_benchmark.txt");
	}
	
	/**
	 * Entry point
	 * @param args
	 */
	public static void main(String[] args) {		
		MATSim4UrbanSimInteroplation m4ui = new MATSim4UrbanSimInteroplation(new String[]{"/Users/thomas/Development/opus_home/data/psrc_parcel/results/ersa_after_submission/config/psrc_parcel_matsim_config_ERSA_Interploation.xml"});
//		MATSim4UrbanSimInteroplation m4ui = new MATSim4UrbanSimInteroplation(new String[]{"/Users/thomas/Development/opus_home/data/seattle_parcel/results/interpolationQuickTest/seattle_parcel_interpolation_config_quick.xml"});
		m4ui.runMATSim();		
	}
	
}
