package playground.santiago.utils;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;


public class CountsCorrecter {

	String badCountsDir = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/counts/counts_merged_VEH_C01.xml";
	String goodCountsDir = "../../../shared-svn/projects/santiago/scenario/inputForMATSim/counts/corrected_counts_merged_VEH_C01.xml";
	
	public static void main(String args[]){
		
		CountsCorrecter cc = new CountsCorrecter();
		cc.Run();
		
	}
	
	public void Run(){
		
		processAndCorrectCounts();
		
	}
	
	public void processAndCorrectCounts(){
		
		Counts <Link> badCounts = new Counts();
		Counts <Link> goodCounts = new Counts ();
		

		
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(badCounts);

		
		
		reader.readFile(badCountsDir);	

		goodCounts.setDescription(badCounts.getDescription());
		goodCounts.setName(badCounts.getName());
		goodCounts.setYear(badCounts.getYear());
		
		for(Count badCount : badCounts.getCounts().values()) {
			//Copy as many things as possible in the goodCounts ... Only ID and name.
			
			Id<Link> linkId = badCount.getId();
			String stationName = badCount.getCsLabel();
			goodCounts.createAndAddCount(linkId, stationName);

		}
		
		for (Count goodCount : goodCounts.getCounts().values() ){
			
			Id<Link> goodCountId = goodCount.getId();
			
				Count badCount = badCounts.getCount(goodCountId);

			for (int h = 7 ; h<=23 ; ++h){
				
				
				
				Volume hourlyVolume = badCount.getVolume(h);
				
				goodCount.createVolume(h, hourlyVolume.getValue());
				
			}

		}
		
		CountsWriter writer = new CountsWriter (goodCounts);
		writer.write(goodCountsDir);
	}
	
	
}
