package playground.wrashid.PDES2;

import java.util.ArrayList;

import org.matsim.core.mobsim.jdeqsim.EventLog;
import org.matsim.core.mobsim.jdeqsim.SimulationParameters;
import org.matsim.core.mobsim.jdeqsim.util.CppEventFileParser;



import playground.wrashid.deqsim.PDESStarter2;

public class Main {

	public static void main(String[] args) {
		// the config file comes as input
		
		String baseDir="/data/matsim/wrashid/input/input_67K/";
		args=new String[1];
			
		args[0]= baseDir + "config.xml";
		PDESStarter2.main(args);
	}
}
