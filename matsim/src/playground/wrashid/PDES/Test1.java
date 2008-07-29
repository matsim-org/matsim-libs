package playground.wrashid.PDES;

import java.util.ArrayList;

import junit.framework.TestCase;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.deqsim.PDESStarter;
import playground.wrashid.test.CppEventFileParser;

public class Test1 extends TestCase {

	public static void main(String[] args) {
		// the config file comes as input
		
		String baseDir="C:/data/SandboxCVS/ivt/studies/wrashid/test/test6/";
		args=new String[1];
			
		args[0]= baseDir + "config.xml";
		PDESStarter.main(args);
		//assertEquals(EventLog.compare(eventLog1,eventLog2),true);
	}
	
	public void testTest1() {
		String baseDir="C:/data/SandboxCVS/ivt/studies/wrashid/test/test6/";
		String[] args=new String[1];
			
		args[0]= baseDir + "config.xml";
		PDESStarter.main(args);
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
