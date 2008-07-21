package playground.wrashid.test.test2;

import java.util.ArrayList;

import junit.framework.TestCase;



import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.test.CppEventFileParser;

public class Test2 extends TestCase {


	
	public void testTest2() {
		//Test2.main(null);
		String baseDir="C:/data/SandboxCVS/ivt/studies/wrashid/test/test2/";
		String[] args=new String[1];
		
		args[0]= baseDir + "config.xml";
		DEQSimStarter.main(args);
		
		
		/*
		
		args[0]= baseDir + "deq_events.txt";
		CppEventFileParser.main(args);
		
		ArrayList<EventLog> eventLog1= SimulationParameters.eventOutputLog;
		
		ArrayList<EventLog> eventLog2= CppEventFileParser.eventLog;
		
		EventLog.print(eventLog1);
		
		assertEquals(EventLog.absAverageLinkDiff(eventLog1,eventLog2)<SimulationParameters.maxAbsLinkAverage,true);
	*/	
		//EventLog.filterEvents(106733,eventLog1,eventLog2);
		
		//assertEquals(EventLog.compare(eventLog1,eventLog2),true);
	}
	
	

}
