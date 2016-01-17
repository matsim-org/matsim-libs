package playground.sergioo.specialTrips2013;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class SpecialTripsGenerator {

	private enum Type1 {
		CHECK,
		TOURISM;
	}
	private enum Type2 {
		WOODLANDS_ARRIVING,
		WOODLANDS_LEAVING,
		TUAS_ARRIVING,
		TUAS_LEAVING;
	}
	
	private static class Trip {
		private Type1 type1;
		private Type2 type2;
		private Person person;
		public Trip(Type1 type1, Type2 type2, Person person) {
			super();
			this.type1 = type1;
			this.type2 = type2;
			this.person = person;
		}
	}
	private static class Distribution {
		private double[] values;

		public int getPosition() {
			double a=0, r=Math.random();
			for(int i = 0; i<values.length; i++) {
				a+=values[i];
				if(r<a)
					return i;
			}
			throw new RuntimeException();
		}
	}
	
	/**
	 * @param args
	 * 0 - Population file
	 * 1 - Network file
	 * 2 - Distributions file
	 * 3 - Start time
	 * 4 - End time
	 * 5 - Bin size
	 * 6 - Out population file
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimPopulationReader(scenario)).readFile(args[0]);
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(args[1]);
		System.out.println(scenario.getPopulation().getPersons().size());
		List<Trip> trips = new ArrayList<Trip>();
		int tourismSum = 0;
		double[] hist = new double[120];
		for(Person person:scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Type1 type1 = null;
			Type2 type2 = null;
			if(((Activity)plan.getPlanElements().get(0)).getFacilityId().toString().contains("TWOOD")) {
				type1 = Type1.CHECK;
				type2 = Type2.WOODLANDS_ARRIVING; 
			}
			else if(((Activity)plan.getPlanElements().get(0)).getFacilityId().toString().contains("TTUAS")) {
				type1 = Type1.CHECK;
				type2 = Type2.TUAS_ARRIVING; 
			}
			else if(((Activity)plan.getPlanElements().get(2)).getFacilityId().toString().contains("TWOOD")) {
				type1 = Type1.CHECK;
				type2 = Type2.WOODLANDS_LEAVING; 
			}
			else if(((Activity)plan.getPlanElements().get(2)).getFacilityId().toString().contains("TTUAS")) {
				type1 = Type1.CHECK;
				type2 = Type2.TUAS_LEAVING; 
			}
			else {
				type1 = Type1.TOURISM;
				hist[(int) (((Leg)plan.getPlanElements().get(1)).getDepartureTime()/900)]++;
				tourismSum++;
			}
			trips.add(new Trip(type1, type2, person));
		}
		for(int i=0; i<hist.length; i++)
			hist[i]/=tourismSum;
		BufferedReader br = new BufferedReader(new FileReader(args[2]));
		double startTime = new Double(args[3]);
		double endTime = new Double(args[4]);
		double binSize = new Double(args[5]);
		br.readLine();
		Distribution tourismDist = new Distribution();
		tourismDist.values = hist;
		Distribution[] distributions = new Distribution[Type2.values().length];
		for(int i = 0; i<distributions.length; i++) {
			distributions[i] = new Distribution();
			distributions[i].values = new double[(int) ((endTime-startTime)/binSize)];
		}
		int pos=0;
		for(double time = startTime; time<endTime; time+=binSize) {
			String[] parts = br.readLine().split(",");
			for(int i=1; i<parts.length; i++)
				distributions[i-1].values[pos]=new Double(parts[i]);
			pos++;
		}
		for(Trip trip:trips)
			if(trip.type1.equals(Type1.CHECK))
				((LegImpl)trip.person.getSelectedPlan().getPlanElements().get(1)).setDepartureTime(getRandomTime(startTime, endTime, binSize, distributions[trip.type2.ordinal()%2].getPosition()));
			else if(trip.type1.equals(Type1.TOURISM) && ((LegImpl)trip.person.getSelectedPlan().getPlanElements().get(1)).getDepartureTime()>86400)
				((LegImpl)trip.person.getSelectedPlan().getPlanElements().get(1)).setDepartureTime(getRandomTime(0, 24*3600, 900, tourismDist.getPosition()));
		(new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())).write(args[6]);
	}

	private static double getRandomTime(double startTime, double endTime, double binSize, int position) {
		return (int)(startTime+(position*binSize)+binSize*Math.random());
	}

}
