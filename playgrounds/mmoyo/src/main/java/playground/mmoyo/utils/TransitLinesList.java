package playground.mmoyo.utils;

import java.io.IOException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.mmoyo.io.TextFileWriter;

/**List all lines id's of a transit Schedule*/
public class TransitLinesList {
	final TransitSchedule schedule;
	
	public TransitLinesList(final TransitSchedule schedule) {
		this.schedule = schedule;
	}
	
	private Set<Id<TransitLine>> getLines(){
		return this.schedule.getTransitLines().keySet();
	}

	private void saveLines(final String outFile){
		StringBuffer sBuff = new StringBuffer();
		final String lcg = "\n";
		for (Id id :this.getLines()){
			sBuff.append(id + lcg);
		}
		new TextFileWriter().write(sBuff.toString(), outFile, false);
	}
	
	private void printLinesIds(){
		for (Id id :this.getLines()){
			System.out.println(id);
		}
	}
	
	private void countLines_Routes(){
		int linesN= 0;
		int routesN= 0;
		final String TAB = "\t";
		for (TransitLine line :this.schedule.getTransitLines().values()){
			System.out.println(line.getId());
			for (TransitRoute route : line.getRoutes().values()){
				System.out.println(TAB + route.getId());
				routesN++;
			}
			linesN++;
		}
		System.out.println("lines :" + linesN);
		System.out.println("routes :" + routesN);
	}
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException  {
		final String SCHEDULEFILE = "../../transitSchedule.xml.gz";
		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(SCHEDULEFILE);
		TransitLinesList transitLinesList = new TransitLinesList(schedule);
		transitLinesList.printLinesIds();
		transitLinesList.countLines_Routes();
	}

}
