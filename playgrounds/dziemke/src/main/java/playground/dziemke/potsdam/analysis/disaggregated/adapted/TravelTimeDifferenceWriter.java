package playground.dziemke.potsdam.analysis.disaggregated.adapted;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class TravelTimeDifferenceWriter {

	 public static void writeToFile(String filename, Map <Id, Double> travelTimeDifference  ) {
	        
		try {
			FileWriter fw = new FileWriter(filename);
			fw.append("Id" + ";" + "difference" + ";\n");
		
			for (Id i : travelTimeDifference.keySet()){
				fw.append(i + ";" + travelTimeDifference.get(i) +";\n");
			}
									
			fw.flush();
			fw.close();
		} catch (IOException e1) {
				e1.printStackTrace();
		}
	}
}