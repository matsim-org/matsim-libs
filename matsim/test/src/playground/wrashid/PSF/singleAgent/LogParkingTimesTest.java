package playground.wrashid.PSF.singleAgent;

import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.parking.LogParkingTimes;

public class LogParkingTimesTest extends MatsimTestCase {
	public void testLogParkingTime() {
		Controler controler=new Controler("test/input/playground/wrashid/PSF/singleAgent/" + "config.xml");
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);
		controler.getConfig().controler().setOutputDirectory(this.getOutputDirectory());
		
		controler.getEvents().addHandler(new LogParkingTimes());
		
		controler.run();
	}
}
