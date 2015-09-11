package playground.wrashid.nan.extended;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;


public class MainDensityAnalysisWithPt {

	public static void main(String[] args) {
//		String networkFile="C:/Users/zheng/Desktop/MATSIM RUNS/DATA/Artemis_Congested/output_network.xml";
//		String eventsFile="C:/Users/zheng/Desktop/MATSIM RUNS/DATA/Artemis_Congested3/50.events.txt.gz";
//		String networkFile="C:/Users/zheng/Desktop/MATSIM RUNS/DATA/Artemis_Congested2/output_network.xml";
		//String eventsFile="C:/Users/zheng/Desktop/MATSIM RUNS/DATA/Artemis_Congested3/it.50/50.events.txt.gz";
		//Coord center=null; // center=null means use all links
		boolean isOldEventFile=false;
		int binSizeInSeconds=60;
		
//		String networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
//		Coord center=new CoordImpl(0,0);
		//boolean isOldEventFile=false;
		
		
		
		String networkFile="C:/data/zhengn-backup-may-2013/zhengn/OUTPUT_JUNE/cordon_pricing_normal_50iters(rough links)/output_network.xml.gz";
		String eventsFile="C:/data/zhengn-backup-may-2013/zhengn/OUTPUT_JUNE/cordon_pricing_normal_50iters(rough links)/ITERS/it.50/50.events.txt.gz";


		Coord center= new Coord(683400.75, 247500.0687);
		//Coord center=new CoordImpl(683139.3125,247387.9531);// high density center
		double radiusInMeters=2000;
		
		//String networkFile="C:/Users/zheng/Desktop/RashidJAR/output/config_10pct_test518/output_network.xml.gz";
		//String eventsFile="C:/Users/zheng/Desktop/RashidJAR/output/config_10pct_test518/ITERS/it.10/10.events.txt.gz";
		
		
				
		Map<Id<Link>, Link> links = NetworkReadExample.getNetworkLinks(networkFile,center,radiusInMeters);// input/set center and radius
		//Map<Id, Link> borderLinks = findBorderLinks.getNetworkLinks(networkFile, center, radiusInMeters);
		
		InFlowInfoCollectorWithPt inflowHandler=new InFlowInfoCollectorWithPt(links,isOldEventFile,binSizeInSeconds); 
		OutFlowInfoCollectorWithPt outflowHandler=new OutFlowInfoCollectorWithPt(links,isOldEventFile,binSizeInSeconds);// "links" makes run faster
		
		inflowHandler.reset(0);
		outflowHandler.reset(0);
				
		EventsManagerImpl events = new EventsManagerImpl();  //create new object of events-manager class
		
		events.addHandler(inflowHandler); // add handler
		events.addHandler(outflowHandler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
	
		reader.readFile(eventsFile); //where we find events data
		
		HashMap<Id<Link>, int[]> linkInFlow = inflowHandler.getLinkInFlow();	//get the matrix
		HashMap<Id<Link>, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();	
		
		HashMap<Id<Link>, int[]> deltaFlow = deltaFlow(linkInFlow, linkOutFlow);
		HashMap<Id<Link>, double[]> density = calculateDensity(deltaFlow,links);
		
		printDensity(density,links);
		//printBorderLinks(borderLinks);
		

		
	}
	
	public static HashMap<Id<Link>, int[]> deltaFlow(HashMap<Id<Link>, int[]> linkInFlow,HashMap<Id<Link>, int[]> linkOutFlow){
		
		HashMap<Id<Link>, int[]> result=new HashMap<>();
		for (Id<Link> linkId:linkInFlow.keySet())	{
			int[] inflowBins = linkInFlow.get(linkId);
			int[] outflowBins = linkOutFlow.get(linkId);
			int[] deltaflowBins = new int[inflowBins.length];
			result.put(linkId, deltaflowBins);// put them into result arrays
			for (int i=0;i<inflowBins.length;i++){
				
				if (deltaflowBins==null || inflowBins==null || outflowBins==null){
					System.out.println();
				}				
				deltaflowBins[i]=inflowBins[i]-outflowBins[i]; //during each interval, nr. of enters
					
			}
			if (linkId.equals(Id.create(126323, Link.class))){
				System.out.println();
			}
		}	
		return result;
	}
	
	public static HashMap<Id<Link>, double[]> calculateDensity(HashMap<Id<Link>, int[]> deltaFlow, Map<Id<Link>, Link> links){
		//send actual link info.)
	HashMap<Id<Link>, double[]> density=new HashMap<Id<Link>, double[]>();
	
	for (Id<Link> linkId:deltaFlow.keySet()){
		density.put(linkId,null);
	}
	
	double networkLength=0;
	int u=0;
	
	for (Id<Link> linkId:density.keySet()){
		
		Link link = links.get(linkId);  
		networkLength = networkLength+(link.getNumberOfLanes())*(link.getLength()); 
		//calculate total network length, this is for later computing the network density          
	}
	for (Id<Link> linkId:density.keySet()){	
		int setAggregationLevel=5; //unit minutes, dont forget to set aggregation level !!!!
		int[] deltaflowBins = deltaFlow.get(linkId);//give labels to deltaflowBins
		double[] densityBins = new double[deltaflowBins.length];  
		double[] densityAggregation = new double[(deltaflowBins.length)/setAggregationLevel+1];  
		Link link = links.get(linkId);
		densityBins[0]=deltaflowBins[0];
		//calculate the number of agents still on the link (denoted as qi here)
		//for each time interval
		for (int i=1;i<deltaflowBins.length;i++){
			densityBins[i]=(densityBins[i-1]+deltaflowBins[i]);
			//density.put(linkId,densityBins);	
			//nr. of agents on the link at interval i+1, plus new enters; 
			//densitybins: number of vehicles on the link at interval i
		}
				
		for (int i=1;i<deltaflowBins.length;i++){
			densityBins[i]=densityBins[i]*1;//000/networkLength;// change the unit of density, from link to network
			densityBins[i]=densityBins[i]/link.getLength()/link.getNumberOfLanes()*1000;
			double sumAggregation=0;
			//the following part of the loop is  to compute the average density for a given interval
			//e.g.,we calculate our basic density in unit agents/minute, if one wants a 5-min aggregation
			//one gets five 1-min density values, and average them 
			if(i%setAggregationLevel==0){  
				for(int j=1;j<setAggregationLevel;j++){
				sumAggregation=sumAggregation+densityBins[i-setAggregationLevel+j];
				}
				densityAggregation[i/setAggregationLevel]= sumAggregation/setAggregationLevel;
			}
		}
		density.put(linkId,densityAggregation);
		deltaFlow.remove(linkId);
	}
	
	return density;
}

public static void printDensity(HashMap<Id<Link>, double[]> density, Map<Id<Link>, Link> links) { // print
	for (Id<Link> linkId : density.keySet()) {
		double[] bins = density.get(linkId);
		Link link = links.get(linkId);
		System.out.print(linkId + " - " + link.getCoord() + ": ");
		//System.out.print(link.getLength()*link.getNumberOfLanes());
		for (int i = 0; i < bins.length; i++) {
			System.out.print(bins[i] + "\t");
		}
		System.out.println();
	}
}

public static void printBorderLinks(Map<Id<Link>, Link> borderLinks) { // print
	for (Id<Link> linkId : borderLinks.keySet()) {
		Link link = borderLinks.get(linkId);
		System.out.print("borderlink:" + linkId + " - " + link.getCoord() + ": ");
		//System.out.print(link.getLength()*link.getNumberOfLanes());
		System.out.println();
	} 
}

}