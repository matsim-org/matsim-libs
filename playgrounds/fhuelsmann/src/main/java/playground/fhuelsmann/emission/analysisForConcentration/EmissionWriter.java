package playground.fhuelsmann.emission.analysisForConcentration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

/**
 * @author friederike
 *
 */

public class EmissionWriter {
	
	private static final Logger logger = Logger.getLogger(EmissionWriter.class);

	public <T> void writeHour2Link2Emissions(
			SortedSet<String> listOfPollutants,
			Map<Double,Map<Id<T>,Map<String, Double>>> time2EmissionMapToAnalyze,
			Network network,
			String outFile){
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("hour \t linkId \t");
			for (String pollutant : listOfPollutants){
				out.append(pollutant + "\t");
			}
			out.append("\n");
			for (Double endOfTimeInterval : time2EmissionMapToAnalyze.keySet()){
				Map<Id<T>,Map<String,Double>> time2value = time2EmissionMapToAnalyze.get(endOfTimeInterval);
				for(Id<T> linkId : time2value.keySet()){
					Map<String, Double> linkId2value = time2value.get(linkId);
						out.append(endOfTimeInterval/3600 +"\t"+ linkId +"\t");
							for(String pollutant : listOfPollutants){
							out.append(linkId2value.get(pollutant) + "\t");
							}
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

