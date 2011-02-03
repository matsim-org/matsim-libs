package air;

import org.matsim.core.controler.Controler;

public class SfAirController {

	/**
	 * @param args
	 * @author fuerbas
	 */
	public static void main(String[] args) {

		Controler con = new Controler(args);		//args: configfile
		con.setOverwriteFiles(true);
		con.run();
		
	}

}
