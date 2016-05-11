package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.io.File;
import java.io.FileOutputStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.roadpricing.CalcPaidToll;

import com.google.inject.Inject;

import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.example.roadpricing.RoadpricingState;
import floetteroed.utilities.math.Vector;

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
		final PTState ptstate = (PTState) state;
		for (Id<Person> personId : ptstate.getPersonIdView()) {
			final Plan selectedPlan = ptstate
				.getSelectedPlan(personId);
			result -= selectedPlan.getScore();
		}
		int currenttotal = scenario.getTransitVehicles().getVehicles().size();
		int added = currenttotal-totalVeh;
		if(added>0){
			for(int i=0; i<added; i++){
				result = result + 4.5;//Thats an adverse efffect on total score
			}
		}
		else if (added<0){
			added=Math.abs(added);
			for(int i=0; i<added;i++){
				result = result - 4.5;//Thats a positive effect due to deleting vehicles
			}
		}
		result /= ptstate.getPersonIdView().size();
		str = str + result	+ "\n";
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
