package playground.mzilske.deteval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;

public class BiogemeWriter {
	
	
	private static final String BIOGEME_OUTPUT_FILE = "../detailedEval/pop/befragte-personen/mode_choices.txt";
	
	private PrintWriter biogemeFileWriter;
	
	private Map<Leg, Double> leg2travelDistance = new HashMap<Leg, Double>();

	private Map<Leg, Leg> clustering = new HashMap<Leg, Leg>();
	
	// private Population populationWithRoutedPlans;
	
	private Population populationWithSurveyData;
	
	private Households households;
	
	public BiogemeWriter(Population populationWithSurveyData, Households households) {
		this.populationWithSurveyData = populationWithSurveyData;
		// this.populationWithRoutedPlans = populationWithRoutedPlans;
		this.households = households;
	}

	public void writeBiogemeFile() throws FileNotFoundException, IOException {
		biogemeFileWriter = new PrintWriter(IOUtils.getBufferedWriter(BIOGEME_OUTPUT_FILE, false));
		writeBiogemeHeader();
		for (Entry<Id, Household> entry: households.getHouseholds().entrySet()) {
			Household household = entry.getValue();
			for (Id personId : household.getMemberIds()) {
				Person personWithSurveyData = populationWithSurveyData.getPersons().get(personId);
				// Person personWithRoutedPlan = populationWithRoutedPlans.getPersons().get(personId);
				Plan planWithSurveyData = personWithSurveyData.getPlans().iterator().next();
				// Plan routedPlan = personWithRoutedPlan.getPlans().iterator().next();
				for (PlanElement planElement : planWithSurveyData.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg legWithSurveyData = (Leg) planElement;
						int legIdx = planWithSurveyData.getPlanElements().indexOf(legWithSurveyData);
						// Leg routedLeg = (Leg) routedPlan.getPlanElements().get(legIdx);
						writeChoiceLineIfPossible(household, personId, legWithSurveyData, legIdx);
					}
				}
			}
		}
		biogemeFileWriter.close();
	}

	private void writeChoiceLineIfPossible(Household household, Id personId, Leg legWithSurveyData, int legIdx) {
		double householdIncome = household.getIncome().getIncome();
		double personalIncome = distributeHouseholdIncomeToMembers(household);
		DecimalFormat decimalFormat = new DecimalFormat("00");
		String id = household.getId().toString() + "000" + decimalFormat.format(household.getMemberIds().indexOf(personId) +1);
		if (legWithSurveyData.getMode() == TransportMode.car) {
			Leg substitutePtLeg = clustering.get(legWithSurveyData);
			Double distance = leg2travelDistance.get(legWithSurveyData);
			if (substitutePtLeg != null && distance != null && distance < 99990 ) {
				int choice = 1;
				double t_car = legWithSurveyData.getTravelTime();
				double t_pt = substitutePtLeg.getTravelTime();
				if (!Double.isNaN(t_car) && !Double.isNaN(t_pt)) {
					// double freeSpeedCarTravelTime = routedLeg.getTravelTime();
					double c_car = distance2carCost(distance);
					double c_pt = distance2ptCost(distance);
					writeBiogemeLine(choice, id, legIdx, t_car, t_pt, c_car, c_pt, householdIncome, personalIncome);
				} else {
					System.out.println("NaN");
				}
			}
		} else if (legWithSurveyData.getMode() == TransportMode.pt) {
			Leg substituteCarLeg = clustering.get(legWithSurveyData);
			Double distance = leg2travelDistance.get(substituteCarLeg);
			if (substituteCarLeg != null && distance != null && distance < 99990) {
				int choice = 2;
				double t_pt = legWithSurveyData.getTravelTime();
				double t_car = substituteCarLeg.getTravelTime();
				if (!Double.isNaN(t_car) && !Double.isNaN(t_pt)) {
					double c_car = distance2carCost(distance);
					double c_pt = distance2ptCost(distance);
					writeBiogemeLine(choice, id, legIdx, t_car, t_pt, c_car, c_pt, householdIncome, personalIncome);
				} else {
					System.out.println("NaN");
				}
			}
		}
	}

	// this distributes the household income on its members - in this case equally...
	private double distributeHouseholdIncomeToMembers(Household household) {
		double personalIncome = household.getIncome().getIncome() / household.getMemberIds().size();
		return personalIncome;
	}
	
	private void writeBiogemeLine(int choice, String biogemeId, int legIdx, double t_car,
			double t_pt, double c_car, double c_pt, double householdIncome, double personalIncome) {
		NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
		numberFormat.setMaximumFractionDigits(2);
		if (householdIncome != -1 && t_car < 999990) {
			biogemeFileWriter.println(choice + " " + biogemeId + " " + legIdx + " " + t_car
					+ " " + t_pt + " " + numberFormat.format(c_car)
					+ " " + numberFormat.format(c_pt) + " " + householdIncome + " " + personalIncome);
		}
	}
	
	
	private void writeBiogemeHeader() {
		biogemeFileWriter.println("choice id legIdx t_car t_pt c_car c_pt household_income personal_income");
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
