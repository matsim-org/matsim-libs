package org.matsim.contrib.freight.usecases.chessboard;

import org.matsim.core.controler.Controler;

@Deprecated
final class RunPassengerPlansFromConfig {
	// yyyy this does not work any more.  Not secured by a testcase.  I think that it is only there to have an example to run
	// matsim _without_ freight.  --> imo, remove.  kai, jan'19

	public static void main(String[] args) {
		String configFile = "input/usecases/chessboard/passenger/config.xml" ;
		Controler controler = new Controler( configFile ) ;	
		controler.run() ;
	}

}
