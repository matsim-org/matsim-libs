package playground.mmoyo.utils;

import java.io.IOException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.mmoyo.io.TextFileWriter;

/**List all lines id's of a transit Schedule*/
public class TransitLinesList {
	final TransitSchedule schedule;
	
	public TransitLinesList(final TransitSchedule schedule) {
		this.schedule = schedule;
	}
	
	public Set<Id> getLines(){
		return this.schedule.getTransitLines().keySet();
	}

	public void saveLines(final String outFile){
		StringBuffer sBuff = new StringBuffer();
		final String lcg = "\n";
		for (Id id :this.getLines()){
			sBuff.append(id + lcg);
		}
		new TextFileWriter().write(sBuff.toString(), outFile, false);
	}
	
	public void printLinesIds(){
		for (Id id :this.getLines()){
			System.out.println(id);
		}
	}
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException  {
		final String SCHEDULEFILE = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(SCHEDULEFILE);
		TransitLinesList transitLinesList = new TransitLinesList(schedule);
		transitLinesList.printLinesIds();
	}

}
