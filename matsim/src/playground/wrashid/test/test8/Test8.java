package playground.wrashid.test.test8;

import java.util.ArrayList;

import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.test.CppEventFileParser;

public class Test8 extends MatsimTestCase {

	/**
	 * @param args
	 */
	

	
	public void testTest8() {
		String baseDir="src/playground/wrashid/test/test8/";
		String[] args=new String[1];
			
		args[0]= baseDir + "config.xml";
		DEQSimStarter.main(args);
		
		args[0]= baseDir + "deq_events.txt";
		CppEventFileParser.main(args);
		
		ArrayList<EventLog> eventLog1= SimulationParameters.eventOutputLog;
		
		ArrayList<EventLog> eventLog2= CppEventFileParser.eventLog;
		
		EventLog.print(eventLog1);
		
		assertEquals(EventLog.absAverageLinkDiff(eventLog1,eventLog2)<SimulationParameters.maxAbsLinkAverage,true);
		
		//assertEquals(EventLog.compare(eventLog1,eventLog2),true);
	}
}
