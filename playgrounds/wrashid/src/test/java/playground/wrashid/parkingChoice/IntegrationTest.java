package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

import junit.framework.TestCase;

public class IntegrationTest  extends TestCase {

	public static boolean someErrorHappened=false;
	public static ParkingModule parkingModule;
		
	public void testModeChange(){
		ParkingChoiceLib.isTestCaseRun=true;
		Controler controler=new Controler("test/input/playground/wrashid/parkingChoice/utils/chessConfig5.xml");
		
		LinkedList<Parking> parkingCollection= new LinkedList<Parking>();
		
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i * 1000 + 500, j * 1000 + 500));
				parking.setMaxCapacity(30000.0);
				parkingCollection.add(parking);
			}
		}
		
		parkingModule=new ParkingModule(controler, parkingCollection);

		AfterMobsimListener afterMobSimListener=new AfterMobsimListener() {
			
			@Override
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				for (Person p: event.getControler().getPopulation().getPersons().values()){
					Plan plan=p.getSelectedPlan();
					if (isPlanCarTripFree(plan)){
						if (parkingModule.getParkingManager().getNumberOfParkedVehicles()>0){
							someErrorHappened=true;
						}
					}
				}
			}
		};
		
		controler.addControlerListener(afterMobSimListener);
		
		//TODO: remove next line...
		controler.setOverwriteFiles(true);
		
		controler.run();
		assertFalse(someErrorHappened);
	}
	
	private boolean isPlanCarTripFree(Plan plan){
		for (PlanElement pe:plan.getPlanElements()){
			if (pe instanceof LegImpl){
				LegImpl leg=(LegImpl) pe;
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
					return false;
				}
			}
		}
		return true;
	}
	
	
	
}
