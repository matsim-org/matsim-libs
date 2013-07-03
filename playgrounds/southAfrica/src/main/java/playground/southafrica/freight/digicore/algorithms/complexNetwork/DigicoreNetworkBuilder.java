package playground.southafrica.freight.digicore.algorithms.complexNetwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

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
	 * 		<li> absolute path of the folder containing the {@link DigicoreVehicle}
	 * 			 files, i.e. XML files;
	 * 		<li> the absolute path of the graph's output file;
	 * 		<li> the absolute path of the graph vertices' order output file;
	 * 	    <li> the absolute path of the file containing the vehicle {@link Id}s
	 * 			 for which the network must be built. If "null", then all vehicle
	 * 			 files will be used to create the network. (Note: currently MUST
	 *           be null - JWJ 20130702)
	 * 		<li> boolean indicating <code>true</code> if only activities with a
	 * 			 facility {@link Id} must be considered as nodes, <code>false</code>
	 * 		  	 otherwise.
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
		
		String inputfolder = args[0];
		
		DigicoreNetworkBuilder dfgb = new DigicoreNetworkBuilder();

		/* Determine which vehicles must be used to build the network. */
		/* FIXME Change FileUtils to only sample files for a given list of 
		 * vehicle Ids. */
		List<File> fileList = FileUtils.sampleFiles(new File(inputfolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
			
		boolean cleanNetwork = Boolean.parseBoolean(args[4]);
		
		/* Checks if a filter facility is provided. */
		Object filter = null;
		if(args.length > 5){
			File f = new File(args[5]);
			if(f.exists() && f.canRead() && f.isFile()){
				filter = dfgb.readFilterList(f.getAbsolutePath());
			} else{
				if(args[5].length() > 0){
					filter = new IdImpl(args[4]);									
				}
			}
		}
		
		LOG.info(" Total number of files to process: " + fileList.size());
		Counter xmlCounter = new Counter("   Vehicles completed: ");
		int dummyCounter = 0;
		for(File f : fileList){
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(f.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			for(DigicoreChain dc : dv.getChains()){
				if( dfgb.checkChain(dc, filter) ){
					for(int i = 0; i < dc.size() - 1; i++){
						DigicoreActivity origin = dc.getAllActivities().get(i);
						DigicoreActivity destination = dc.getAllActivities().get(i+1);
						if(cleanNetwork){
							if(origin.getFacilityId() != null && destination.getFacilityId() != null){
								dfgb.network.addArc(origin, destination);
							}
						} else{
							/* Build a network from ALL links, not just between `interesting' facilities, i.e.
							 * those that were clustered into formal facilities.
							 */
							Id originId = origin.getFacilityId() != null ? origin.getFacilityId() : new IdImpl("d" + dummyCounter++);
							Id destinationId = destination.getFacilityId() != null ? destination.getFacilityId() : new IdImpl("d" + dummyCounter++);
							origin.setFacilityId(originId);
							destination.setFacilityId(destinationId);
							dfgb.network.addArc(origin, destination);
						}
					}						
				}
			}
			xmlCounter.incCounter();
		}
		xmlCounter.printCounter();
		dfgb.writeGraphStatistics();
		

		/* Write the output. */
		DigicoreNetworkWriter dnw = new DigicoreNetworkWriter(dfgb.getNetwork());
		try {
			dnw.writeNetwork(args[1], false);
		} catch (IOException e) {
			LOG.error(e.getMessage());
			LOG.error("Couldn't write network.");
		}
//		try {
//			dnw.writeGraphOrderToFile(args[2]);
//		} catch (IOException e) {
//			LOG.error("Couldn't write network order.");
//		}
		
		Header.printFooter();
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
	 * @return
	 */
	public boolean checkChain(DigicoreChain chain, Object filter){
		boolean check = false;
		if(filter == null){
			check = true;
		} else if(filter instanceof Id){
			check = chain.containsFacility((Id) filter);
		} else if(filter instanceof List<?>){
			@SuppressWarnings("unchecked")
			List<Id> list = (List<Id>)filter;
			boolean found = false;
			int i = 0;
			while(!found && i < list.size()){
				found = chain.containsFacility(list.get(i));
				i++;
			}
			check = found;
		} 
		return check;
	}
	
	public DigicoreNetwork getNetwork(){
		return this.network;
	}	

	
	public List<Id> readFilterList(String filename){
		List<Id> list = new ArrayList<Id>();
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while((line = br.readLine()) != null){
				if(line.contains(" ")){
					throw new IllegalArgumentException("Id contains spaces!! Not allowed.");
				}
				list.add(new IdImpl(line));
			}
		} catch (IOException e) {
			throw new RuntimeException("IOException when reading from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("IOException when closing BufferedReader " + filename);
			}
		}
		
		return list;
	}
	
	
	
	public void writeGraphStatistics(){
		LOG.info(" Preparing statistics...");
		
		int[] minMax = this.getNetwork().getMinMaxEdgeWeights();
				
		LOG.info("---------------------  Graph statistics  -------------------");
		LOG.info("      Number of arcs: " + network.getEdgeCount());
		LOG.info("  Number of vertices: " + network.getVertexCount());
		LOG.info("             Density: " + String.format("%01.6f", this.getNetwork().getDensity()));
		LOG.info(" Minimum edge weight: " + minMax[0]);
		LOG.info(" Maximum edge weight: " + minMax[1]);
		LOG.info("------------------------------------------------------------");
	}
	
	
	
		
	
	

}
