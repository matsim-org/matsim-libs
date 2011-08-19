package playground.fhuelsmann.emission.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class EmissionCongestionWriter {
	private static final Logger logger = Logger.getLogger(EmissionCongestionWriter.class);

	void writeLinkLocation2Emissions(
			SortedSet<String> listOfPollutants,
			Map<Double,Map<Id, Map<String, Double>>> emissionCongestion,
			Network network,
			String outFile){
		try{
			FileWriter fstream = new FileWriter(outFile);			
			BufferedWriter out = new BufferedWriter(fstream);
			out.append("time\tlinkId\txLink\tyLink\t");
			for (String pollutant : listOfPollutants){
				out.append(pollutant + "[g]\t");
			}
			out.append("\n");
			for (Double time : emissionCongestion.keySet()){
				Map<Id,Map<String, Double>> time2value = emissionCongestion.get(time);
			
			
				for(Id linkId : time2value.keySet()){
					Link link = network.getLinks().get(linkId);
					Coord linkCoord = link.getCoord();
					Double xLink = linkCoord.getX();
					Double yLink = linkCoord.getY();

					out.append(time/3600 +"\t"+ linkId + "\t" + xLink + "\t" + yLink + "\t");

					Map<String, Double> link2value = time2value.get(linkId);
					for(String pollutant : listOfPollutants){
					out.append(link2value.get(pollutant) + "\t");
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
