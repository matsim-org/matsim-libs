package playground.sergioo.specialTrips2013;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class SpecialTripsAnalyzer {

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
		public Type1 type1;
		public Type2 type2;
		public double time;
		public Trip(Type1 type1, Type2 type2, double time) {
			super();
			this.type1 = type1;
			this.type2 = type2;
			this.time = time;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimPopulationReader(scenario)).readFile(args[0]);
		System.out.println(scenario.getPopulation().getPersons().size());
		List<Trip> trips = new ArrayList<Trip>();
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
			else
				type1 = Type1.TOURISM;
			double time = ((Leg)plan.getPlanElements().get(1)).getDepartureTime();
			trips.add(new Trip(type1, type2, time));
		}
		double timeSlice = new Double(args[1]);
		List<Double> all = new ArrayList<Double>(), check = new ArrayList<Double>(), tourist = new ArrayList<Double>();
		List<Double> ta = new ArrayList<Double>(), tl = new ArrayList<Double>(), wa = new ArrayList<Double>(), wl = new ArrayList<Double>();
		for(Trip trip:trips) {
			all.add(trip.time/3600);
			if(trip.type1.equals(Type1.CHECK)) {
				check.add(trip.time/3600);
				if(trip.type2.equals(Type2.TUAS_ARRIVING))
					ta.add(trip.time/3600);
				else if(trip.type2.equals(Type2.TUAS_LEAVING))
					tl.add(trip.time/3600);
				else if(trip.type2.equals(Type2.WOODLANDS_ARRIVING))
					wa.add(trip.time/3600);
				else if(trip.type2.equals(Type2.WOODLANDS_LEAVING))
					wl.add(trip.time/3600);
			}
			else
				tourist.add(trip.time/3600);
		}
		double[] allArray = new double[all.size()], checkArray = new double[check.size()], touristArray = new double[tourist.size()];
		for(int i=0; i<all.size(); i++)
			allArray[i] = all.get(i);
		for(int i=0; i<check.size(); i++)
			checkArray[i] = check.get(i);
		for(int i=0; i<tourist.size(); i++)
			touristArray[i] = tourist.get(i);
		HistogramDataset dataset = new HistogramDataset();
		double endTime = 30;
		int numSeries = (int)(endTime*3600/timeSlice);
		//dataset.addSeries("Tourist", touristArray, numSeries, 0.0, 30.0);		
		dataset.addSeries("Check", checkArray, numSeries);//, 0.0, endTime);
		dataset.addSeries("Total", allArray, numSeries);//, 0.0, endTime);
		JFreeChart chart = ChartFactory.createHistogram( "Time histogram special trips 25%", "time", "number", dataset, PlotOrientation.VERTICAL, true, false, false);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new ChartPanel(chart));
		frame.pack();
		frame.setVisible(true);
	}

}
