package playground.wrashid.ABMT.vehicleShare;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;

public class WriteStatistics implements IterationEndsListener {

		
		

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "statistics.txt");

		try(PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) {
		    writer.println("Person \t hasEV \t distTrav \t toll");
		    writer.close();
		}catch (IOException e) {
		    System.out.println("Unable to write statistics");
		}		
		
		
		for (Id id : DistanceTravelledWithCar.distanceTravelled.keySet()){
			
			if (DistanceTravelledWithCar.distanceTravelled.get(id)<0){
				System.out.println();
				DebugLib.stopSystemWithError();
			}
			
			try(PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) {
			    writer.println(id + "\t" + VehicleInitializer.personHasElectricVehicle.get(id) + "\t" + DistanceTravelledWithCar.distanceTravelled.get(id) + "\t" + TollsManager.tollDisutilities.get(id));
			    writer.close();
			}catch (IOException e) {
			    System.out.println("Unable to write statistics");
			}
			
			
		}
		
		VehicleInitializer.prepareForNewIteration();
		
	}

}
