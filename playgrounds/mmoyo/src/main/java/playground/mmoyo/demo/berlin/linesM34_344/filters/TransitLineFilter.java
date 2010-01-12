package playground.mmoyo.demo.berlin.linesM34_344.filters;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.xml.sax.SAXException;
import org.matsim.transitSchedule.api.TransitLine;

/**selects only the given lines of a transitSchedule and creates a sub-schedule of it*/
public class TransitLineFilter {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException  {
		final String SCHEDULEFILE = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_10x_900s_bignetwork/transitSchedule.networkOevModellBln.xml.gz";
		final String NETWORKFILE  = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_10x_900s_bignetwork/network.multimodal.xml.gz";
		final String FILTERED_SCHEDULE_FILE = "../playgrounds/mmoyo/output/filteredSchedule.xml";
		//final String[] linesArray = {"BVB----M44", "BVB----344"};
		final String[] linesArray = {"S45", "S46", "S47", "BVU----U7", "BVU----U8", "BVB----M11", "BVB----M29", "BVB----M41", "BVB----M44", "BVB----M46", "BVB----X11", "BVB----104",  "BVB----167", "BVB----170",  "BVB----171", "BVB----172", "BVB----181", "BVB----194", "BVB----246", "BVB----277", "BVB----344", "735", "736",  "BVB----N8"};
		
		NetworkLayer network= new NetworkLayer(new NetworkFactoryImpl());
		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = builder.createTransitSchedule();
		TransitSchedule filteredTransitSchedule = builder.createTransitSchedule();
		
		//reads the transitSchedule file
		new MatsimNetworkReader(network).readFile(NETWORKFILE);
		new TransitScheduleReaderV1(transitSchedule, network).readFile(SCHEDULEFILE);
		
		System.out.println ("List of lines");		
		for (TransitLine line : transitSchedule.getTransitLines().values()){
			System.out.println(line.getId());
		}
		
		//filter, deleting the rest of lines
		for (int i=0 ; i<linesArray.length ; i++){
			Id idLine=  new IdImpl(linesArray[i]);
			TransitLine line= transitSchedule.getTransitLines().get(idLine);
			if (line==null){
				System.out.println("the line does not exist: " + linesArray[i]);
			}
			for (TransitRoute route: line.getRoutes().values()){
				for (TransitRouteStop stop: route.getStops()){
					if (!filteredTransitSchedule.getFacilities().values().contains(stop.getStopFacility())) {
						filteredTransitSchedule.addStopFacility(stop.getStopFacility());
					}
				}
			}
			filteredTransitSchedule.addTransitLine(line);
		}
		
		new TransitScheduleWriterV1(filteredTransitSchedule).write(FILTERED_SCHEDULE_FILE);
	}
	

}
