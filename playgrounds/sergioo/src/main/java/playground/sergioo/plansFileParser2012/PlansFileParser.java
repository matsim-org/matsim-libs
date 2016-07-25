package playground.sergioo.plansFileParser2012;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansFileParser {
	
	private static final String CSV_SEPARATOR = ",";
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
		matsimNetworkReader.readFile(args[0]);
		final Population plans = scenario.getPopulation();
		final MatsimReader matsimPlansReader = new PopulationReader(scenario);
		matsimPlansReader.readFile(args[1]);
		
		double totalTime = Double.parseDouble(args[2]);
		double binTime = Double.parseDouble(args[3]);
		double[][] travelTimeBins = new double[(int) Math.ceil(totalTime/binTime)][3];
		for(double time = 0; time < totalTime; time+=binTime)
			travelTimeBins[(int)Math.floor(time/binTime)][0] = time;
		for(Person person:plans.getPersons().values()) {
			Plan plan = (Plan)person.getSelectedPlan();
			for(Activity activity = PopulationUtils.getFirstActivity( plan );!activity.equals(PopulationUtils.getLastActivity(plan));) {
				final Activity act = activity;
				Leg leg = PopulationUtils.getNextLeg(plan, act);
				int numBin = (int) Math.floor(leg.getDepartureTime()/binTime);
				travelTimeBins[numBin][1]=travelTimeBins[numBin][1]+leg.getTravelTime();
				travelTimeBins[numBin][2]++;
				final Leg leg1 = leg;
				activity = PopulationUtils.getNextActivity(plan, leg1);
			}
		}
		PrintWriter writer = new PrintWriter(new File(args[4]));
		for(double[] travelTimeBin:travelTimeBins)
			writer.println(travelTimeBin[0]+CSV_SEPARATOR+travelTimeBin[1]+CSV_SEPARATOR+travelTimeBin[2]);
		writer.close();
	}

}
