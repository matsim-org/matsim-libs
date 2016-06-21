package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import floetteroed.opdyts.DecisionVariableRandomizer;

class PTScheduleRandomiser implements DecisionVariableRandomizer<PTSchedule> {
	private final PTSchedule ptschedule;
	private final TransitScheduleAdapter adapter;
	private final Scenario scenario;
	String str = "";
	PTScheduleRandomiser(final Scenario scenario, final PTSchedule decisionVariable) {
		this.ptschedule=decisionVariable;
		this.adapter = new TransitScheduleAdapter();
		this.scenario=scenario;
		// TODO Auto-generated method stub
	}

	@Override
	public Collection<PTSchedule> newRandomVariations(
			PTSchedule decisionVariable) {
		TransitSchedule schedule = this.ptschedule.schedule;
		Vehicles vehicles = this.ptschedule.vehicles;
		final List<PTSchedule> result = new ArrayList<>(2);
		//Ensuring two independent copies of the existing schedule, and not changing with in the current schedule
//		result.add(adapter.updateScheduleAdd(scenario, adapter.deepCopyVehicles(vehicles), adapter.deepCopyTransitSchedule(schedule)));//Randomly add vehicles
//		result.add(adapter.updateScheduleRemove(scenario, adapter.deepCopyVehicles(vehicles), adapter.deepCopyTransitSchedule(schedule)));//Randomly remove vehicles
		result.add(adapter.updateScheduleDeleteRoute(scenario, adapter.deepCopyVehicles(vehicles), adapter.deepCopyTransitSchedule(schedule)));//Randomly delete routes
		result.add(adapter.updateScheduleAddRoute(scenario, adapter.deepCopyVehicles(vehicles), adapter.deepCopyTransitSchedule(schedule)));//Randomly delete routes
//		result.add(adapter.updateScheduleDeleteRoute(scenario, adapter.deepCopyVehicles(vehicles), adapter.deepCopyTransitSchedule(schedule)));//Randomly delete routes

		
		int size = vehicles.getVehicles().size() - adapter.getUnusedVehs(schedule);
		int sizeadded = result.get(0).vehicles.getVehicles().size() - adapter.getUnusedVehs(result.get(0).schedule);
		int sizedeleted = result.get(1).vehicles.getVehicles().size() - adapter.getUnusedVehs(result.get(1).schedule);
		str = str + size + "		" +
				sizeadded + "		" + 
				sizedeleted + "		" + "\n";
		writeToTextFile(str, "vehicles.txt");//Write the number of vehicles statistics to a file
		System.out.println("Vehicles Written to: " + scenario.getConfig().controler()
				.getOutputDirectory());
		return result;
	}
	
	public void writeToTextFile(String str, String path){
		try { 
			File file=new File(path);
			FileOutputStream fileOutputStream=new FileOutputStream(file);
		    fileOutputStream.write(str.getBytes());
		    fileOutputStream.close();
	       
	    } catch(Exception ex) {
	        //catch logic here
	    }
	}
}
