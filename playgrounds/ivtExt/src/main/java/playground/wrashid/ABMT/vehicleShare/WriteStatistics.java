package playground.wrashid.ABMT.vehicleShare;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;

public class WriteStatistics implements IterationEndsListener {

		
		

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		ArrayList<String> list=new ArrayList<String>();
		
		String fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "statistics.txt");

		list.add("Person \t hasEV \t distTrav \t toll \t tollEntry \t tollExit");
		
//		try(PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) {
//		    writer.println("Person \t hasEV \t distTrav \t toll \t tollEntry \t tollExit");
//		    writer.close();
//		}catch (IOException e) {
//		    System.out.println("Unable to write statistics");
//		}		
//		
		
		
		
		
		for (Id id : DistanceTravelledWithCar.distanceTravelled.keySet()){
			
			if (DistanceTravelledWithCar.distanceTravelled.get(id)<0){
				System.out.println();
				DebugLib.stopSystemWithError();
			}
			
//			try(PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) {
//			    //writer.println(id + "\t" + VehicleInitializer.personHasElectricVehicle.get(id) + "\t" + DistanceTravelledWithCar.distanceTravelled.get(id) + "\t" + TollsManager.tollDisutilities.get(id) + "\t" + TollsManager.tollTimeOfEntry.get(id) + "\t" + TollsManager.tollTimeOfExit.get(id));
//			    //writer.close();
//			}catch (IOException e) {
//			    System.out.println("Unable to write statistics");
//			}
//			
			
			StringBuilder sb=new StringBuilder();
			sb.append(id);
			sb.append("\t");
			sb.append(VehicleInitializer.personHasElectricVehicle.get(id));
			sb.append("\t");
			sb.append(DistanceTravelledWithCar.distanceTravelled.get(id));
			sb.append("\t");
			sb.append(TollsManager.tollDisutilities.get(id));
			sb.append("\t");
			sb.append(TollsManager.tollTimeOfEntry.get(id));
			sb.append("\t");
			sb.append(TollsManager.tollTimeOfExit.get(id));
			
			list.add(sb.toString());
		}
		
		GeneralLib.writeList(list, fileName);
		
		VehicleInitializer.prepareForNewIteration();
		
	}

}
