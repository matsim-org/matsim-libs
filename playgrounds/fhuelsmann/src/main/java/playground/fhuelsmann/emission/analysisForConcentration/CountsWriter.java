package playground.fhuelsmann.emission.analysisForConcentration;

/**
 * @author friederike
 *
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

public class CountsWriter {
	
	private static final Logger logger = Logger.getLogger(CountsWriter.class);

	public <T> void writeHour2Link2Counts(
			Map<Double, Map<Id<T>, Double[]>>time2CountsTotalFiltered,
			Network network,
			String outFile){
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("endOfTimeInterval\t linkId\t freeSpeed\t allVehicles\t hdv\t length\t");
			out.append("\n");
			for (Double endOfTimeInterval : time2CountsTotalFiltered.keySet()){
				Map<Id<T>, Double[]> time2value = time2CountsTotalFiltered.get(endOfTimeInterval);
				for(Id<T> linkId : time2value.keySet()){
				out.append(endOfTimeInterval/3600 +"\t"+ linkId +"\t");

				Double [] link2value = time2value.get(linkId);
				
				out.append(link2value[0] + "\t"+ link2value[1] + "\t" + link2value[2] + "\t"+link2value[3] + "\t");
					
				out.append("\n");
				}
			}
			//Close the output stream
			out.close();
			logger.info("Finished writing output to " + outFile);
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

}
