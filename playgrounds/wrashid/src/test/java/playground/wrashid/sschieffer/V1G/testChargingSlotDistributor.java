package playground.wrashid.sschieffer.V1G;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.junit.Test;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;

public class testChargingSlotDistributor extends MatsimTestCase{
	
	String configPath="test/input/playground/wrashid/sschieffer/config.xml";
	
		public static DecentralizedV1G decentralizedV1G;
	
	@Test  public void testDecentralizedV1G(){
	
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		Controler controler=new Controler(configPath);
		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin(controler);
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		controler.addControlerListener(new EnergyConsumptionInit());
		controler.addControlerListener(eventHandlerAtStartupAdder);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new IterationEndsListener() {
					
		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			
		try {
			decentralizedV1G = new DecentralizedV1G(event.getControler());
		
		} catch (Exception e) {
		e.printStackTrace();
		}
		}
		});
	
		controler.run();

	}
	
	
	public void testAssignChargingScheduleForParkingInterval(){
		PolynomialFunction func= new PolynomialFunction(new double[]{10,150});
		double joulesInInterval=1000;
		double startTime=0;
		double endTime= 100;
		double chargingTime=40;
	}
	
	
	
}
