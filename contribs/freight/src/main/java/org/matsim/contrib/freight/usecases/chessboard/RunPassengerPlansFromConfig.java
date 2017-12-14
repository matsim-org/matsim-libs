package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.core.controler.Controller;

public class RunPassengerPlansFromConfig {
	
	public static void main(String[] args) {
		String configFile = "input/usecases/chessboard/passenger/config.xml" ;
		Controller controler = new Controller( configFile ) ;	
		controler.run() ;
	}

}
