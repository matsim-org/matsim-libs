package playground.wrashid.nan;

import java.util.HashMap;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class MainDensityAnalysis {

	public static void main(String[] args) {
		String networkFile="H:/data/experiments/Mohit/10pct ZH/output_network.xml.gz";
		String eventsFile="H:/data/experiments/Mohit/10pct ZH/ITERS/it.50/50.events.txt.gz";
		Coord center=null; // center=null means use all links
		boolean isOldEventFile=false;
		int binSizeInSeconds=3600;
		
//		String networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
//		Coord center=new CoordImpl(0,0);
		//boolean isOldEventFile=false;
		
		double radiusInMeters=50000;
		
		
		
		
		Map<Id, Link> links = NetworkReadExample.getNetworkLinks(networkFile,center,radiusInMeters);// input/set center and radius
		InFlowInfoCollector inflowHandler=new InFlowInfoCollector(links,isOldEventFile,binSizeInSeconds); 
		OutFlowInfoCollector outflowHandler=new OutFlowInfoCollector(links,isOldEventFile,binSizeInSeconds);// "links" makes run faster
		
		inflowHandler.reset(0);
		outflowHandler.reset(0);
				
		EventsManagerImpl events = new EventsManagerImpl();  //create new object of events-manager class
		
		events.addHandler(inflowHandler); // add handler
		events.addHandler(outflowHandler);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
	
		reader.readFile(eventsFile); //where we find events data
		
		HashMap<Id, int[]> linkInFlow = inflowHandler.getLinkInFlow();	//get the matrix
		HashMap<Id, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();	
		
		HashMap<Id, int[]> deltaFlow = deltaFlow(linkInFlow, linkOutFlow);
		HashMap<Id, double[]> density = calculateDensity(deltaFlow,links);
		
		printDensity(density,links);
		
	}
	
	public static HashMap<Id, int[]> deltaFlow(HashMap<Id, int[]> linkInFlow,HashMap<Id, int[]> linkOutFlow){
		
		HashMap<Id, int[]> result=new HashMap<Id, int[]>();
		for (Id linkId:linkInFlow.keySet())	{
			int[] inflowBins = linkInFlow.get(linkId);
			int[] outflowBins = linkOutFlow.get(linkId);
			int[] deltaflowBins = new int[inflowBins.length];
			result.put(linkId, deltaflowBins);// put them into result arrays
			for (int i=0;i<inflowBins.length;i++){
				
				if (deltaflowBins==null || inflowBins==null || outflowBins==null){
					System.out.println();
				}
				
				deltaflowBins[i]=inflowBins[i]-outflowBins[i];
				
				
				
			}
			if (linkId.equals(new IdImpl(126323))){
				System.out.println();
			}
		}	
		
		return result;
	}
	
	public static HashMap<Id, double[]> calculateDensity(HashMap<Id, int[]> deltaFlow, Map<Id, Link> links){
			//send actual link info.)
		HashMap<Id, double[]> density=new HashMap<Id, double[]>();
		
		for (Id linkId:deltaFlow.keySet()){
			density.put(linkId,null);
		}
		
		
		for (Id linkId:density.keySet()){
			
			if (linkId.equals(new IdImpl(126216))){
				System.out.println();
			}
			
			int[] deltaflowBins = deltaFlow.get(linkId);//give labels to deltaflowBins
			double[] densityBins = new double[deltaflowBins.length];
			Link link = links.get(linkId);
			densityBins[0]=deltaflowBins[0];
			for (int i=1;i<deltaflowBins.length;i++){
				densityBins[i]=(densityBins[i-1]+deltaflowBins[i]);
			}
			
			for (int i=1;i<deltaflowBins.length;i++){
				densityBins[i]=densityBins[i]/link.getLength()*1000;
			}
			
			density.put(linkId,densityBins);
			deltaFlow.remove(linkId);
		}
		
		return density;
	}
	
	public static void printDensity(HashMap<Id, double[]> density, Map<Id, Link> links) { // print
		for (Id linkId : density.keySet()) {
			double[] bins = density.get(linkId);

			Link link = links.get(linkId);

			System.out.print(linkId + " - " + link.getCoord() + ": ");

			for (int i = 0; i < bins.length; i++) {
				System.out.print(bins[i] + "\t");
			}

			System.out.println();
		}
	}
	
}

