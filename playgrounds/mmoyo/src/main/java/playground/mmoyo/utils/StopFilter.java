package playground.mmoyo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

/**
 * Compare transit stop attributes from a counts file and a transit schedule
 */
public class StopFilter {
	
	private void filterOutVolumesZeroCounts(Counts counts){
		List<Id> counts2RemoveList = new ArrayList<Id>();

		for(Entry<Id<Link>, Count> entry: counts.getCounts().entrySet() ){
			Count count = entry.getValue();
			if(count.getVolume(1).getValue()==0.0){
				Id<Link> countId = entry.getKey();
				counts2RemoveList.add(countId);
			}
		}
		
		for (Id id : counts2RemoveList){
			counts.getCounts().remove(id);
		}
	}
	
	public static void main(String[] args) {
		String countsFile = "../../";
		
		DataLoader dataLoader = new DataLoader();
		Counts counts = dataLoader.readCounts(countsFile);
	
		new StopFilter().filterOutVolumesZeroCounts(counts);
		
		File file =new File(countsFile);
		new CountsWriter(counts).write(file.getParentFile().getPath()+ "/Filtered" + file.getName());
	}
}
