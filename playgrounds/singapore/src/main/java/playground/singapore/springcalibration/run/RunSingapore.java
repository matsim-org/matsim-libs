package playground.singapore.springcalibration.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

public class RunSingapore {	
	private final static Logger log = Logger.getLogger(RunSingapore.class);

	public static void main(String[] args) {
		log.info("Running SingaporeControlerRunner");
				
		Controler controler = new Controler(args[0]);
		
		log.info("Adding controler listener");
		controler.addControlerListener(new CountsControlerListenerSingapore());		
		controler.run();
		log.info("finished SingaporeControlerRunner");
	}

}
