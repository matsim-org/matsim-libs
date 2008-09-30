package playground.wrashid.PDES1;

import java.util.ArrayList;



import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.PDESStarter1;
import playground.wrashid.tryouts.starting.CppEventFileParser;

public class Test2 {

	public static void main(String[] args) {
		// the config file comes as input
		
		String baseDir="/home/wrashid/matsim2/input_67K/";
		args=new String[1];
			
		args[0]= baseDir + "config.xml";
		PDESStarter1.main(args);
		//assertEquals(EventLog.compare(eventLog1,eventLog2),true);
	}
}
