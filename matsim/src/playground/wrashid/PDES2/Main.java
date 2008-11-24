package playground.wrashid.PDES2;

import java.util.ArrayList;



import playground.wrashid.DES.EventLog;
import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.deqsim.PDESStarter2;
import playground.wrashid.tryouts.starting.CppEventFileParser;

public class Main {

	public static void main(String[] args) {
		// the config file comes as input
		
		String baseDir="/data/matsim/wrashid/input/input_67K/";
		args=new String[1];
			
		args[0]= baseDir + "config.xml";
		PDESStarter2.main(args);
	}
}
