package playground.mmoyo.demo.X5;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.mmoyo.demo.ScenarioPlayer;

public class Player {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		
		//invoke controler first
		/*
		String networkFile = "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/network.xml";
		String scheduleFile =  "test/input/org/matsim/transitSchedule/TransitScheduleReaderTest/transitSchedule.xml";
		String outputPlansFile = "test/input/playground/marcel/pt/plans.xml";  //output of controller, input of player 
		String outputDirectory = "./output/transitIntegrationTest";
		*/
		
		String networkFile = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5X5/network.xml";
		String scheduleFile =  "../shared-svn/studies/schweiz-ivtch/pt-experimental/5X5/simple1TransitSchedule.xml";
		String outputPlansFile = "./output/X5demo/output_plans.xml";  //output of controller, input of player 
		String outputDirectory = "./output/X5PlayerOutput";
		
		
		ScenarioPlayer.main(new String[]{networkFile, scheduleFile, outputPlansFile, outputDirectory});
	
	}
}
