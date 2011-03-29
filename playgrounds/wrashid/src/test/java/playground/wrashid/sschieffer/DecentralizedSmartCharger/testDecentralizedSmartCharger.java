package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;

public class testDecentralizedSmartCharger extends TestCase{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final LinkedListValueHashMap<Id, Vehicle> vehicles;
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
				
		final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		final double emissionPerLiterEngine = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		final double bufferBatteryCharge=0.0;
		
		final double MINCHARGINGLENGTH=5*60;//5 minutes
		
		Controler controler=new Controler(configPath);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		
		final EnergyConsumptionInit e= new EnergyConsumptionInit(
				phev, ev, combustion);
				
		
		controler.addControlerListener(e);
		
		
				
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							parkingTimesPlugin,
							e.getEnergyConsumptionPlugin(),
							outputPath, 
							MINCHARGINGLENGTH, 
							e.getVehicles(),
							gasJoulesPerLiter,
							emissionPerLiterEngine);
					
					
					
					//myDecentralizedSmartCharger.run();
					
					myDecentralizedSmartCharger.readAgentSchedules();
					
										
					//myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules()
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
							
			}
		});
		
				
		controler.run();		
				
	}

}


