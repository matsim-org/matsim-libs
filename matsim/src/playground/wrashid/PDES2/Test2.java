package playground.wrashid.PDES2;

import java.util.ArrayList;



import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.PDESStarter2;
import playground.wrashid.test.CppEventFileParser;

public class Test2 {

	public static void main(String[] args) {
		// the config file comes as input
		
		String baseDir="/home/wrashid/matsim2/input_67K/";
		args=new String[1];
			
		args[0]= baseDir + "config.xml";
		PDESStarter2.main(args);
		//assertEquals(EventLog.compare(eventLog1,eventLog2),true);
	}
}
