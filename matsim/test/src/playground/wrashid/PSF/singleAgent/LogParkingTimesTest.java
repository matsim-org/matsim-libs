package playground.wrashid.PSF.singleAgent;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.parking.LogParkingTimes;
import playground.wrashid.PSF.parking.ParkLog;
import playground.wrashid.PSF.parking.ParkingTimes;

public class LogParkingTimesTest extends MatsimTestCase {
	public void testLogParkingTime() {
		Controler controler=new Controler("test/input/playground/wrashid/PSF/singleAgent/" + "config.xml");
		controler.addControlerListener(new AddEnergyScoreListener());
		//controler.setOverwriteFiles(true);
		//controler.getConfig().controler().setOutputDirectory(this.getOutputDirectory());
		
		LogParkingTimes logParkingTimes=new LogParkingTimes(controler);		
		controler.addControlerListener(new SimulationStartupListener(logParkingTimes));
		
		controler.run();
		
		
		ParkingTimes parkingTimes=logParkingTimes.getParkingTimes().get(new IdImpl("1"));
		
		// allow small delta of one second (because the output time in the log file is truncated
		assertEquals(parkingTimes.getCarLastTimeParked(),61449,1);
		ParkLog parkLog=parkingTimes.getParkingTimes().get(0);
		assertEquals(parkLog.getStartParkingTime(),22989,1);
		assertEquals(parkLog.getEndParkingTime(),59349,1);
	}
}
