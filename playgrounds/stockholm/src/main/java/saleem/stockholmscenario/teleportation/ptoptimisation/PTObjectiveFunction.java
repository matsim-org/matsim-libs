package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.io.File;
import java.io.FileOutputStream;

import opdytsintegration.MATSimState;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;


/**
 * Returns the negative sum of the scores of the selected plans of all agents,
 * excluding toll.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PTObjectiveFunction implements ObjectiveFunction {
	String str = "";
	int totalVeh = 36813;
	Scenario scenario;
	public PTObjectiveFunction(Scenario scenario){
		this.scenario=scenario;
	}
	@Override
	public double value(SimulatorState state) {//Simple summation of selected plan scores
		double result = 0;
		// TODO Auto-generated method stub
		final MATSimState ptstate = (MATSimState) state;
		for (Id<Person> personId : ptstate.getPersonIdView()) {
			final Plan selectedPlan = ptstate
				.getSelectedPlan(personId);
			result -= selectedPlan.getScore();
		}
		int currenttotal = scenario.getTransitVehicles().getVehicles().size();
		ScenarioHelper helper = new ScenarioHelper();
		int unusedvehs = helper.getUnusedVehs(scenario.getTransitSchedule());
		int added = currenttotal-totalVeh-unusedvehs;
		if(added>0){
			for(int i=0; i<added; i++){
				result = result + 4.0;//Thats an adverse effect on total score
			}
		}
		else if (added<0){
			added=Math.abs(added);
			for(int i=0; i<added;i++){
				result = result - 4.0;//Thats a positive effect due to deleting vehicles
			}
		}
		int vehs = currenttotal-unusedvehs;
		result /= ptstate.getPersonIdView().size();
		str = str + result	+ "\t" + vehs + "\n";
		writeToTextFile(str, "scores.txt");
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
