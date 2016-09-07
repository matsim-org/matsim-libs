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
		ScenarioHelper helper = new ScenarioHelper();
		final List<PTSchedule> result = new ArrayList<>(2);
		//Ensuring two independent copies of the existing schedule, and not changing with in the current schedule
//		result.add(adapter.updateScheduleAdd(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));//Randomly add vehicles
//		result.add(adapter.updateScheduleRemove(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));//Randomly remove vehicles
		
		/*Must always be updateScheduleDeleteRoute first, as the transit schedule with deleted routes is changed,
		 *  with 2X probability to add routes to the transit schedule. One X for balancing the deleted routes, 
		 *  one X for creating a variation with added routes. We have to work like this for adding routes and lines, 
		 *  due to the way plans are memorised in the optimisation process.
		 */
		double factorline = 0.1, factorroute=0.3;
		
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		
//		
//		result.add(adapter.updateScheduleDeleteLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), 0.01));
//		result.add(adapter.updateScheduleDeleteLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), 0.01));
//		result.add(adapter.updateScheduleDeleteLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), 0.01));
//		
//		result.add(adapter.updateScheduleAddLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), 0.01));
//		result.add(adapter.updateScheduleAddLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), 0.01));
//		result.add(adapter.updateScheduleAddLines(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles), 0.01));
//		
		
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//
//		result.add(adapter.updateScheduleAdd(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));
//		result.add(adapter.updateScheduleAdd(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));
//		result.add(adapter.updateScheduleAdd(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));
//		
//		result.add(adapter.updateScheduleRemove(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));
//		result.add(adapter.updateScheduleRemove(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));
//		result.add(adapter.updateScheduleRemove(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule)));
		
		result.add(adapter.updateScheduleChangeCapacity(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		result.add(adapter.updateScheduleChangeCapacity(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		result.add(adapter.updateScheduleChangeCapacity(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		result.add(adapter.updateScheduleChangeCapacity(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		
		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
//		result.add(new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles)));
		
		
		
//		
//		PTSchedule ptSchedule1 = new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles));
//		PTSchedule ptSchedule2 = new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles));
//		PTSchedule ptSchedule3 = new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles));
//		
//		result.add(adapter.updateScheduleDeleteRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		result.add(adapter.updateScheduleDeleteRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		result.add(adapter.updateScheduleDeleteRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		
//		result.add(ptSchedule1);
//		result.add(ptSchedule2);
//		result.add(ptSchedule3);
//		
//		result.add(adapter.updateScheduleAddRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		result.add(adapter.updateScheduleAddRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		result.add(adapter.updateScheduleAddRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes

//		result.add(adapter.updateScheduleDeleteRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Delete routes
//		PTSchedule ptSchedule = new PTSchedule(scenario, helper.deepCopyTransitSchedule(schedule), helper.deepCopyVehicles(vehicles));
//		ptSchedule.initialise();
//		result.add(ptSchedule);//Randomly Delete routes
//		result.add(adapter.updateScheduleAddRoute(scenario, helper.deepCopyVehicles(vehicles), helper.deepCopyTransitSchedule(schedule), factorline, factorroute));//Randomly Add routes
////		result.add(adapter.updateScheduleDeleteRoute(scenario, adapter.deepCopyVehicles(vehicles), adapter.deepCopyTransitSchedule(schedule)));//Randomly delete routes

		
		int size = vehicles.getVehicles().size() - helper.getUnusedVehs(schedule);
		int sizedeleted = result.get(1).vehicles.getVehicles().size() - helper.getUnusedVehs(result.get(1).schedule);
		int sizeadded = result.get(1).vehicles.getVehicles().size() - helper.getUnusedVehs(result.get(1).schedule);
//		str = str + size + "		" +
//				sizedeleted + "		" + 
//				sizeadded + "		" + "\n";
//		
		str = str + size + "\n";
		
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
