package playground.sergioo.AreaTollSCWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;

public class AreaTollSCWriter {
	
	private static final String TXT_SEPARATOR = "\t";
	private static final double TIME_INTERVAL = 15*60;
	private static final double SECS2CHF = 600;
	
	public static void main(String[] args) throws IOException {
		RoadPricingScheme roadPricingScheme = new RoadPricingScheme();
		roadPricingScheme.setName("Social Cost Toll");
		roadPricingScheme.setDescription("According to the social cost of Nash equilibrium iteration a toll is applied to every link.");
		roadPricingScheme.setType(RoadPricingScheme.TOLL_TYPE_AREA);
		roadPricingScheme.addCost(0, 15/121, 0);
		BufferedReader reader = new BufferedReader(new FileReader(new File("./data/youssef/socialCost.txt")));
		reader.readLine();
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(TXT_SEPARATOR);
			Id linkId = new IdImpl(parts[0]);
			roadPricingScheme.addLink(linkId);
			double time = 0;
			for(int i=1; i<parts.length; i++)
				roadPricingScheme.addLinkCost(linkId, time, time+=TIME_INTERVAL, Double.parseDouble(parts[i])/SECS2CHF);
			line = reader.readLine();
		}
		reader.close();
		RoadPricingWriterXMLv1 writer = new RoadPricingWriterXMLv1(roadPricingScheme);
		writer.writeFile("./data/youssef/socialCostToll.xml");
	}
	
}
