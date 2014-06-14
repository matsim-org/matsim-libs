package usecases.chessboard;

import org.matsim.core.controler.Controler;

public class RunPassengerPlansFromConfig {
	
	public static void main(String[] args) {
		String configFile = "input/usecases/chessboard/passenger/config.xml" ;
		Controler controler = new Controler( configFile ) ;	
		controler.run() ;
	}

}
