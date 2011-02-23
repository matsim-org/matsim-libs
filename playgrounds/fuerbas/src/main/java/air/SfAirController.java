package air;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;

public class SfAirController {

	/**
	 * @param args
	 * @author fuerbas
	 */
	public static void main(String[] args) {

		Controler con = new Controler(args);		//args: configfile
		con.setOverwriteFiles(true);
		ControlerListener lis = new SfFlightTimeControlerListener();
		con.addControlerListener(lis);
		con.run();
	
	}
	


}
