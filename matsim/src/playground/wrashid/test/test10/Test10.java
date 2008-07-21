package playground.wrashid.test.test10;

import java.util.ArrayList;

import junit.framework.TestCase;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.test.CppEventFileParser;

public class Test10 extends TestCase {


	
	public void testTest1() {
		String baseDir="C:/data/SandboxCVS/ivt/studies/wrashid/test/test10/";
		String[] args=new String[1];
			
		args[0]= baseDir + "config.xml";
		DEQSimStarter.main(args);
		/*
		args[0]= baseDir + "deq_events.txt";
		CppEventFileParser.main(args);
		
		ArrayList<EventLog> eventLog1= SimulationParameters.eventOutputLog;
		
		//EventLog.print(eventLog1);
		
		ArrayList<EventLog> eventLog2= CppEventFileParser.eventLog;
		
		
		assertEquals(EventLog.absAverageLinkDiff(eventLog1,eventLog2)<SimulationParameters.maxAbsLinkAverage,true);
		*/
		//assertEquals(EventLog.compare(eventLog1,eventLog2),true);
	}
}
