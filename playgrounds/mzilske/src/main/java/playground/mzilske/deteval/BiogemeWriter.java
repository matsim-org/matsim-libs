package playground.mzilske.deteval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;

public class BiogemeWriter {
	
	
	private static final String BIOGEME_OUTPUT_FILE = "../detailedEval/pop/mode_choices.txt";
	
	private PrintWriter biogemeFileWriter;
	
	private Map<Leg, Double> leg2travelDistance = new HashMap<Leg, Double>();

	private Map<Leg, Leg> clustering = new HashMap<Leg, Leg>();
	
	private Scenario scenario;
	
	private Households households;
	
	public BiogemeWriter(Scenario scenario, Households households) {
		this.scenario = scenario;
		this.households = households;
	}

	public void writeBiogemeFile() throws FileNotFoundException, IOException {
		biogemeFileWriter = new PrintWriter(IOUtils.getBufferedWriter(BIOGEME_OUTPUT_FILE, false));
		writeBiogemeHeader();
		for (Entry<Id, Household> entry: households.getHouseholds().entrySet()) {
			Household household = entry.getValue();
			for (Id personId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(personId);
				Plan plan = person.getPlans().iterator().next();
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						int legIdx = plan.getPlanElements().indexOf(leg);
						writeChoiceLineIfPossible(household, personId, leg, legIdx);
					}
				}
			}
		}
		biogemeFileWriter.close();
	}

	private void writeChoiceLineIfPossible(Household household, Id personId, Leg leg,
			int legIdx) {
		double income = household.getIncome().getIncome();
		String id = personId.toString() + "." + legIdx;
		if (leg.getMode() == TransportMode.car) {
			Double distance = leg2travelDistance.get(leg);
			if (distance != null && distance < 99990 ) {
				int choice = 1;
				double t_car = leg.getTravelTime();
				if (!Double.isNaN(t_car)) {
					double t_pt = carTime2ptTime(leg.getTravelTime());
					double c_car = distance2carCost(distance);
					double c_pt = distance2ptCost(distance);
					writeBiogemeLine(choice, id, t_car, t_pt, c_car, c_pt, income);
				} else {
					System.out.println("NaN");
				}
			}
		} else if (leg.getMode() == TransportMode.pt) {
			Leg substituteCarLeg = clustering.get(leg);
			Double distance = leg2travelDistance.get(substituteCarLeg);
			if (substituteCarLeg != null && distance != null && distance < 99990) {
				int choice = 2;
				double t_pt = leg.getTravelTime();
				double t_car = substituteCarLeg.getTravelTime();
				if (!Double.isNaN(t_car)) {
					double c_car = distance2carCost(distance);
					double c_pt = distance2ptCost(distance);
					writeBiogemeLine(choice, id, t_car, t_pt, c_car, c_pt, income);
				} else {
					System.out.println("NaN");
				}
			}
		}
	}
	
	private void writeBiogemeLine(int choice, String id, double t_car,
			double t_pt, double c_car, double c_pt, double income) {
		NumberFormat numberFormat = NumberFormat.getInstance();
		numberFormat.setMaximumFractionDigits(2);
		if (income != -1 && t_car < 999990) {
			biogemeFileWriter.println(choice + " " + id + " " + t_car
					+ " " + t_pt + " " + numberFormat.format(c_car)
					+ " " + numberFormat.format(c_pt) + " " + income);
		}
	}
	
	
	private void writeBiogemeHeader() {
		biogemeFileWriter.println("choice id t_car t_pt c_car c_pt household_income");
	}

	private double distance2ptCost(double distance) {
		if (0.14 * distance > 10000) {
			throw new RuntimeException();
		}
		return 0.14 * distance;
	}

	private double distance2carCost(double distance) {
		return 0.20 * distance;
	}

	private double carTime2ptTime(double tCar) {
		return 2 * tCar;
	}
	
	public void putTravelDistance(Leg leg, double distance) {

		if (distance > 999990) {
			System.out.println("Strecke falsch.");
		} else {
			
		}
		leg2travelDistance.put(leg, distance);
	}

	public void putProxyLeg(Leg sourceLeg, Leg targetLeg) {
		clustering.put(sourceLeg, targetLeg);
	}

}
