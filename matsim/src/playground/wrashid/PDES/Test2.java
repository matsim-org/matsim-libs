package playground.wrashid.PDES;

import java.util.ArrayList;

import junit.framework.TestCase;

import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.deqsim.PDESStarter;
import playground.wrashid.test.CppEventFileParser;

public class Test2 {

	public static void main(String[] args) {
		// the config file comes as input
		
		String baseDir="/home/wrashid/matsim2/input/";
		args=new String[1];
			
		args[0]= baseDir + "config.xml";
		PDESStarter.main(args);
		//assertEquals(EventLog.compare(eventLog1,eventLog2),true);
	}
}
