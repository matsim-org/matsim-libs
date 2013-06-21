package playground.wrashid.msimoni.analyses;

import org.matsim.api.core.v01.Id;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

public class MainReadTollPrices {

	public static void main(String[] args) {
		RoadPricingSchemeImpl scheme1 = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 reader1 = new RoadPricingReaderXMLv1(scheme1);
		reader1.parse("H:/input/tolls.xml");
		
		for (Id id:scheme1.getTolledLinkIds()){
			System.out.println(id);
		}
	}
	
}
