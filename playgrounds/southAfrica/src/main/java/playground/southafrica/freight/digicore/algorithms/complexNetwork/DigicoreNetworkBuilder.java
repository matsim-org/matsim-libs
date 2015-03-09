package playground.southafrica.freight.digicore.algorithms.complexNetwork;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;

import playground.southafrica.freight.digicore.algorithms.djcluster.DigicoreClusterRunner;
import playground.southafrica.freight.digicore.analysis.postClustering.ClusteredChainGenerator;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreFacility;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;


public class DigicoreNetworkBuilder {
	private final static Logger LOG = Logger.getLogger(DigicoreNetworkBuilder.class);
	private DigicoreNetwork network;
	private long buildStartTime;
	private long buildStopTime;

	
	/**
	 * Implementation of the {@link DigicoreNetworkBuilder}. To run this 
	 * class, vehicle XML files must be available that have been clustered using
	 * {@link DigicoreClusterRunner}, and subsequently cleaned using the class
	 * {@link ClusteredChainGenerator} so that some of the {@link DigicoreFacility}s
	 * have {@link Id}s. This class then builds a directed network graph using
	 * the subsequent occurrences of facility {@link Id}s in {@link DigicoreChain}s.
	 * @param args the following arguments to be provided, and in the following 
	 * 		order:
	 * 	<ol>
	 * 		<li> absolute path of the folder containing the clustering analysis. 
	 * 	         Since the clustering parameters influence the facilities, which
	 * 			 in turn influences the complex network structure, we need to build
	 * 			 the complex network(s) for a specific set of clustering parameters.
	 * 			 Inside each clustering parameter configuration there should be a 
	 * 			 folder titled `xml2' containing the {@link DigicoreVehicle} files;
	 * 		<li> (optional) {@link Id} of a {@link DigicoreFacility} on which the
	 * 			 chains will be filtered. Only necessary, for example, when you
	 * 			 only want to build the network graph using chains that passed 
	 * 			 through a specific gateway, eg. entering Gauteng through the
	 * 			 N3 gateway, or when you want to build the network graph for a 
	 * 			 specific facility like a warehouse. <b><i>NOTE:</i></b> 
	 * 			 <ol>
	 * 				<li> If this argument is a valid absolute path to a txt-file, 
	 * 				which contains one {@link Id} per line, then the network 
	 * 				graph will be built for chains containing <i>any</i> one (or 
	 * 				more) of the list of facilities.
	 * 			 	<li> If <b><i>NO</i></b> filter is required, this argument
	 * 				must be <b><i>omitted</i></b>, and not passed as an empty
	 * 				argument, otherwise an empty network will be created.
	 * 			 </ol> 
	 * 	</ol>
	 * 
	 */
	public static void main(String[] args) {
		Header.printHeader(DigicoreNetworkBuilder.class.toString(), args);
		LOG.info("Memory at start: " + Runtime.getRuntime().totalMemory());
		
		String inputfolder = args[0];
		
		/* These values should be set following Quintin's Design-of-Experiment inputs. */
		double[] radii = {1, 5, 10, 15, 20, 25, 30, 35, 40};
		int[] pmins = {1, 5, 10, 15, 20, 25};

		/* Checks if a filter facility is provided. Either as a readable file,
		 * or as sequential Id arguments. */
		List<Id<ActivityFacility>> filter = null;
		if(args.length > 1){
			File f = new File(args[1]);
			if(f.exists() && f.canRead() && f.isFile()){
				filter = FileUtils.readIds(f.getAbsolutePath(), ActivityFacility.class);
			} else{
				filter = new ArrayList<Id<ActivityFacility>>();
				int numberOfIdArguments = 0;
				for(int i = 1; i < args.length; i++){
					filter.add(Id.create(args[i], ActivityFacility.class));
					numberOfIdArguments++;
				}
				LOG.info("Read " + numberOfIdArguments + " Ids from arguments.");
			}
		}

		/* Build the networks for all configurations. */
		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				/* Just write some indication to the log file as to what we're 
				 * busy with at this point in time. */
				LOG.info("================================================================================");
				LOG.info("Executing complex network building for radius " + thisRadius + ", and pmin of " + thisPmin);
				LOG.info("================================================================================");
				
				/* Determine configuration-specific filenames. */
				String xmlFolder = String.format("%s%.0f_%d/xml2/", inputfolder, thisRadius, thisPmin);
				String networkFile = String.format("%s%.0f_%d/%.0f_%d_network.txt", inputfolder, thisRadius, thisPmin, thisRadius, thisPmin);
				String networkOrderFile = String.format("%s%.0f_%d/%.0f_%d_networkOrder.txt", inputfolder, thisRadius, thisPmin, thisRadius, thisPmin);
				
				/* Get the list of vehicles to use. */
				List<File> fileList = FileUtils.sampleFiles(new File(xmlFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
				
				DigicoreNetworkBuilder dfgb = new DigicoreNetworkBuilder();
				
				/* Gather some memory and time stats, and then build the network. */
				long startTime = System.currentTimeMillis();
				long startMemory = Runtime.getRuntime().totalMemory();
				
				dfgb.buildNetwork(filter, fileList);

				long endTime = System.currentTimeMillis();
				long endMemory = Runtime.getRuntime().totalMemory();
				LOG.info(String.format("Memory: radius - %.0f; pmin - %d; start - %d; end - %d; diff - %d", thisRadius, thisPmin, startMemory, endMemory, endMemory-startMemory));
				LOG.info(String.format("Time: radius - %.0f; pmin - %d; ms - %d", thisRadius, thisPmin, endTime - startTime));
				
				/* Write the output. */
				DigicoreNetworkWriter dnw = new DigicoreNetworkWriter(dfgb.getNetwork());
				try {
					dnw.writeNetwork(networkFile, false);
				} catch (IOException e) {
					LOG.error(e.getMessage());
					LOG.error("Couldn't write network.");
				}
//				try {
//					dnw.writeGraphOrderToFile(networkOrderFile);
//				} catch (IOException e) {
//					LOG.error("Couldn't write network order.");
//				}
				
				/* See if we can get the network file size. Write that to the console. */ 
				File f = new File(networkFile);
				LOG.info(String.format("File size: radius - %.0f; pmin - %d; size - %d", thisRadius, thisPmin, f.getTotalSpace()));
			}
		}
				
		Header.printFooter();
	}


	public void buildNetwork(List<Id<ActivityFacility>> filter, List<File> fileList) {
		LOG.info("Building network... number of vehicle files to process: " + fileList.size());
		this.buildStartTime = System.currentTimeMillis();
		
		Counter xmlCounter = new Counter("   vehicles completed: ");
		for(File f : fileList){
			/* Read the vehicle file. */
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(f.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			
			/* Process vehicle's activities. */
			for(DigicoreChain dc : dv.getChains()){
				if( this.checkChain(dc, filter) ){
					for(int i = 0; i < dc.size() - 1; i++){
						DigicoreActivity origin = dc.getAllActivities().get(i);
						DigicoreActivity destination = dc.getAllActivities().get(i+1);
						if(origin.getFacilityId() != null && destination.getFacilityId() != null){
							this.network.addArc(origin, destination);
						}
					}
				}						
			}
			xmlCounter.incCounter();
		}
		xmlCounter.printCounter();
		this.buildStopTime = System.currentTimeMillis();
		this.writeGraphStatistics();
	}
	
	
	public DigicoreNetworkBuilder() {
		network = new DigicoreNetwork();
	}
		
	
	/**
	 * Performs a check if the given chain contains any activities that occur at
	 * interesting facilities. This may be when:
	 * <ul>
	 * 		<li> no filter is given, i.e. all chains are considered;
	 * 		<li> at least one of the activities in the chain occur at a given 
	 * 			 {@link DigicoreFacility} of interest; or
	 * 		<li> at least one of the activities in the chain occur at any one 
	 * 			 of a list of {@link DigicoreFacility}s.
	 * @param chain
	 * @param filter
	 * @return <code>true</code> if the chain contains facilities of interest, 
	 * 		   <code>false</code> otherwise.
	 */
	public boolean checkChain(DigicoreChain chain, List<Id<ActivityFacility>> filter){
		boolean check = false;
		if(filter == null){
			check = true;
		} else {
			boolean found = false;
			int i = 0;
			while(!found && i < filter.size()){
				found = chain.containsFacility(filter.get(i));
				i++;
			}
			check = found;
		} 
		return check;
	}
	
	
	public DigicoreNetwork getNetwork(){
		return this.network;
	}	

	
	/** 
	 * Writing some predetermined statistics regarding the network, and the 
	 * time taken to build the network. Current statistics include:
	 * <ul>
	 * 		<li> number of arcs;
	 * 		<li> number of vertices;
	 * 		<li> graph density;
	 * 		<li> minimum edge weight;
	 * 		<li> maximum edge weight;
	 * 		<li> time (in seconds) required to build the network.
	 * </ul> */
	public void writeGraphStatistics(){
		LOG.info(" Preparing statistics...");
		
		int[] minMax = this.getNetwork().getMinMaxEdgeWeights();
				
		LOG.info("---------------------  Graph statistics  -------------------");
		LOG.info("         Number of arcs: " + network.getEdgeCount());
		LOG.info("     Number of vertices: " + network.getVertexCount());
		LOG.info("                Density: " + String.format("%01.6f", this.getNetwork().getDensity()));
		LOG.info("    Minimum edge weight: " + minMax[0]);
		LOG.info("    Maximum edge weight: " + minMax[1]);
		LOG.info(" Network build time (s): " + String.format("%.2f", ((double)this.buildStopTime - (double)this.buildStartTime)/1000));
		LOG.info("------------------------------------------------------------");
	}
	
}
