package air.run;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;


public class SfAirController {

	/**
	 * @param args
	 * @author fuerbas
	 */
	public static void main(String[] args) {

		Controler con = new Controler("Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\air_config.xml");		//args: configfile
		con.setOverwriteFiles(true);
		ControlerListener lis = new SfFlightTimeControlerListener();
		con.addControlerListener(lis);
		con.run();
	
	}
	


}
