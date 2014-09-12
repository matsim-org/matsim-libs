package playground.sergioo.hits2012Scheduling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Person.IncomeInterval;

public class IncomeEstimation {
	
	private static Map<String, String> POSTAL_DISTRICTS = new HashMap<String, String>();
	private static List<IncomeInterval> incomeIntervals = new ArrayList<IncomeInterval>();
	private static double[] CONSTANT = new double[]{0, 0.99, -1.27, -5.6, -11.1, -13.9, -16.9, -17.9, -20.6};
	private static double[] BETA_INDIAN = new double[]{0, 0, -0.317, 0, 0, 0.402, 0.672, 0.944, 0.879};
	private static double[] BETA_MALAY = new double[]{0, 0.0249, -0.466, -0.978, -1.04, -1.25, -1.59, -1.85, -1.6};
	private static double[] BETA_OTHER = new double[]{0.458, 0.905, 1.18, 1.18, 1.36, 1.71, 2.05, 2.14};
	private static double[] AGE = new double[]{0, 0.101, 0.242, 0.371, 0.497, 0.570, 0.638, 0.724, 0.734};
	private static double[] AGE_GENDER = new double[]{0, 0.028, 0.0523, 0.046, 0.0295, 0.0383, 0.0399, 0, 0.0461};
	private static double[] AGE_SQ = new double[]{0, -0.00156, -0.00356, -0.00494, -0.00605, 0.00701, -0.00763, -0.00837, -0.00858};
	private static double[] CAR_LIC = new double[]{0, 0.867, 1.42, 1.79, 1.87, 1.78, 1.7, 2.48, 2.15};
	private static double[] DOMESTIC = new double[]{9.27, 0, 0, 0, 0, 0, 0, 0, 0};
	private static double[] EMP_PART = new double[]{0, -2.39, -3.58, -4.16, -4.8, -4.81, -5.76, -6.19, -5.62};
	private static double[] SELF_EMP = new double[]{0, -0.601, -0.561, -0.257, -0.41, 0, 0, 0, 0};
	private static double[] GENDER = new double[]{0, -0.727, -1.33, -1.01, 0, 0, 0, 0, 0};
	private static double[] HDB_LARGE = new double[]{0, 0, 0.372, 0.725, 0.683, 0.809, 0.685, 0.812, 1.17};
	private static double[] HDB_SMALL = new double[]{0, -0.113, -0.538, -0.666, -0.946, -1.08, -0.921, -0.785, -1.05};
	private static double[] LAND_PROP = new double[]{0, 0, 0.457, 0.949, 1.46, 1.53, 1.85, 1.33, 3.24};
	private static double[] CLEANER = new double[]{3.15, 0, 0, 0, 0, 0, 0, 0, 0};
	private static double[] CLERK = new double[]{0, 0.636, -0.423, -1.57, 0, 0, 0, 0, 0};
	private static double[] MACHINE = new double[]{2.61, 1.5, 0, -1.22, 0, 0, 0, 0, 0};
	private static double[] MANAGER = new double[]{0, 2.18, 4.62, 5.67, 7.3, 7.89, 8.47, 6.94, 7.31};
	private static double[] NATIONAL = new double[]{7.31, 0, 0, 0, 0, 0, 0, 0, 0};
	private static double[] OTHER = new double[]{0, 0, -0.915, -0.573, 0.763, -0.0634, 0, 0, 0};
	private static double[] PROD = new double[]{1.96, 1.21, 0.471, -0.0455, 0, 0, 0, 0, 0};
	private static double[] PROF = new double[]{0, -2.7, -1.48, 0, 1.68, 2.18, 2.23, 0, 2.45};
	private static double[] SALES = new double[]{1.21, 0.385, -0.526, -0.776, 0, 0, 0, 0, 0};
	private static double[] UNIF = new double[]{0, 0, 0.245, 0.608, 1.62, 0.863, 2.38, 0, 0};
	private static double[] PC_18_22 = new double[]{0, 0, 0, 0, 0.0694, 0.0463, 0.0641, 0.0117, 0.0761};
	private static double[] PC_8_11 = new double[]{0, 0, 0.232, 0.280, 0.546, 0.711, 0.845, 0.595, 1.46};
	private static double[] PC_3_4_5 = new double[]{0, 0, 0.0439, 0.138, 0.664, 0.971, 1.12, 0.990, 1.88};
	private static double[] PRIVATE_FLAT = new double[]{0, 0, 0.14, 0.572, 1.17, 1.62, 1.95, 2.25, 3.18};
	private static double[] VEH = new double[]{0, 0, 0.542, 0.902, 0.998, 1.34, 1.43, 1.43, 1.78};
	
	public static void init() {
		POSTAL_DISTRICTS.put("01", "01"); POSTAL_DISTRICTS.put("02", "01"); POSTAL_DISTRICTS.put("03", "01"); POSTAL_DISTRICTS.put("04", "01"); POSTAL_DISTRICTS.put("05", "01"); POSTAL_DISTRICTS.put("06", "01");
		POSTAL_DISTRICTS.put("07", "02"); POSTAL_DISTRICTS.put("08", "02");
		POSTAL_DISTRICTS.put("14", "03"); POSTAL_DISTRICTS.put("15", "03"); POSTAL_DISTRICTS.put("16", "03");
		POSTAL_DISTRICTS.put("09", "04"); POSTAL_DISTRICTS.put("10", "04");
		POSTAL_DISTRICTS.put("11", "05"); POSTAL_DISTRICTS.put("12", "05"); POSTAL_DISTRICTS.put("13", "05");
		POSTAL_DISTRICTS.put("17", "06");
		POSTAL_DISTRICTS.put("18", "07"); POSTAL_DISTRICTS.put("19", "07");
		POSTAL_DISTRICTS.put("20", "08"); POSTAL_DISTRICTS.put("21", "08");
		POSTAL_DISTRICTS.put("22", "09"); POSTAL_DISTRICTS.put("23", "09");
		POSTAL_DISTRICTS.put("24", "10"); POSTAL_DISTRICTS.put("25", "10"); POSTAL_DISTRICTS.put("26", "10"); POSTAL_DISTRICTS.put("27", "10");
		POSTAL_DISTRICTS.put("28", "11"); POSTAL_DISTRICTS.put("29", "11"); POSTAL_DISTRICTS.put("30", "11");
		POSTAL_DISTRICTS.put("31", "12"); POSTAL_DISTRICTS.put("32", "12"); POSTAL_DISTRICTS.put("33", "12");
		POSTAL_DISTRICTS.put("34", "13"); POSTAL_DISTRICTS.put("35", "13"); POSTAL_DISTRICTS.put("36", "13"); POSTAL_DISTRICTS.put("37", "13");
		POSTAL_DISTRICTS.put("38", "14"); POSTAL_DISTRICTS.put("39", "14"); POSTAL_DISTRICTS.put("40", "14"); POSTAL_DISTRICTS.put("41", "14");
		POSTAL_DISTRICTS.put("42", "15"); POSTAL_DISTRICTS.put("43", "15"); POSTAL_DISTRICTS.put("44", "15"); POSTAL_DISTRICTS.put("45", "15");
		POSTAL_DISTRICTS.put("46", "16"); POSTAL_DISTRICTS.put("47", "16"); POSTAL_DISTRICTS.put("48", "16");
		POSTAL_DISTRICTS.put("49", "17"); POSTAL_DISTRICTS.put("50", "17"); POSTAL_DISTRICTS.put("81", "17");
		POSTAL_DISTRICTS.put("51", "18"); POSTAL_DISTRICTS.put("52", "18");
		POSTAL_DISTRICTS.put("53", "19"); POSTAL_DISTRICTS.put("54", "19"); POSTAL_DISTRICTS.put("55", "19"); POSTAL_DISTRICTS.put("82", "19");
		POSTAL_DISTRICTS.put("56", "20"); POSTAL_DISTRICTS.put("57", "20");
		POSTAL_DISTRICTS.put("58", "21"); POSTAL_DISTRICTS.put("59", "21");
		POSTAL_DISTRICTS.put("60", "22"); POSTAL_DISTRICTS.put("61", "22"); POSTAL_DISTRICTS.put("62", "22"); POSTAL_DISTRICTS.put("63", "22"); POSTAL_DISTRICTS.put("64", "22");
		POSTAL_DISTRICTS.put("65", "23"); POSTAL_DISTRICTS.put("66", "23"); POSTAL_DISTRICTS.put("67", "23"); POSTAL_DISTRICTS.put("68", "23");
		POSTAL_DISTRICTS.put("69", "24"); POSTAL_DISTRICTS.put("70", "24"); POSTAL_DISTRICTS.put("71", "24");
		POSTAL_DISTRICTS.put("72", "25"); POSTAL_DISTRICTS.put("73", "25");
		POSTAL_DISTRICTS.put("77", "26"); POSTAL_DISTRICTS.put("78", "26");
		POSTAL_DISTRICTS.put("75", "27"); POSTAL_DISTRICTS.put("76", "27");
		POSTAL_DISTRICTS.put("79", "28"); POSTAL_DISTRICTS.put("80", "28");
		incomeIntervals.add(new IncomeInterval(0,1000));
		incomeIntervals.add(new IncomeInterval(1000,2000));
		incomeIntervals.add(new IncomeInterval(2000,3000));
		incomeIntervals.add(new IncomeInterval(3000,4000));
		incomeIntervals.add(new IncomeInterval(4000,5000));
		incomeIntervals.add(new IncomeInterval(5000,6000));
		incomeIntervals.add(new IncomeInterval(6000,7000));
		incomeIntervals.add(new IncomeInterval(7000,8000));
		incomeIntervals.add(new IncomeInterval(8000,20000));
	}
	public static void setIncome(Map<String, Household> households) {
		for(Household household:households.values()) {
			for(Person person: household.getPersons().values())
				if(person.getIncomeInterval()==null)
					setIncome(household, person);
			for(Person person: household.getPersonsNoTraveling().values())
				if(person.getIncomeInterval()==null)
					setIncome(household, person);
		}
	}

	private static void setIncome(Household household, Person person) {
		double maxUtility = -Double.MAX_VALUE;
		int maxI = -1;
		for(int i=0; i<incomeIntervals.size(); i++) {
			double utility = calcUtility(i, household, person); 
			if(utility>maxUtility) {
				maxUtility = utility;
				maxI = i;
			}
		}
		person.setIncomeInterval(incomeIntervals.get(maxI));
	}

	private static double calcUtility(int i, Household household, Person person) {
		double utility = CONSTANT[i];
		if(household.getEthnic().equals("Indian"))
			utility += BETA_INDIAN[i];
		else if(household.getEthnic().equals("Malay"))
			utility += BETA_MALAY[i];
		else if(household.getEthnic().equals("Others"))
			utility += BETA_OTHER[i];
		utility += AGE[i]*person.getAgeInterval().getCenter();
		utility += person.getGender().equals("Male")?AGE_GENDER[i]*person.getAgeInterval().getCenter():0;
		utility += AGE_SQ[i]*Math.pow(person.getAgeInterval().getCenter(), 2);
		utility += person.hasLicence()?CAR_LIC[i]:0;
		if(person.getEmployment().equals("Domestic worker"))
			utility += DOMESTIC[i];
		else if(person.getEmployment().equals("Employed Part-time"))
			utility += EMP_PART[i];
		else if(person.getEmployment().equals("Self-employed"))
			utility += SELF_EMP[i];
		else if(person.getEmployment().equals("National service"))
			utility += NATIONAL[i];
		utility += person.getGender().equals("Male")?GENDER[i]:0;
		if(household.getDwellingType().equals("HDB 1-room") || household.getDwellingType().equals("HDB 2-room") || household.getDwellingType().equals("HDB 3-room"))
			utility += HDB_SMALL[i];
		else if(household.getDwellingType().equals("HDB 5-room") || household.getDwellingType().equals("HDB other than flat") || household.getDwellingType().equals("HUDC flat"))
			utility += HDB_LARGE[i];
		else if(household.getDwellingType().equals("Landed Property"))
			utility += LAND_PROP[i];
		else if(household.getDwellingType().equals("Private flat/condo"))
			utility += PRIVATE_FLAT[i];
		else if(household.getDwellingType().equals("Shophouse") || household.getDwellingType().equals("Floor of shophouse"))
			utility += 0;
		if(person.getOccupation().equals("Cleaner, labourer & related worker"))
			utility += CLEANER[i];
		else if(person.getOccupation().equals("Clerical worker"))
			utility += CLERK[i];
		else if(person.getOccupation().equals("Plant & machine operator & assembler"))
			utility += MACHINE[i];
		else if(person.getOccupation().equals("Legislator,senior official & manager"))
			utility += MANAGER[i];
		else if(person.getOccupation().equals("Production craftsman & related worker"))
			utility += PROD[i];
		else if(person.getOccupation().equals("Professional"))
			utility += PROF[i];
		else if(person.getOccupation().equals("Service & sales worker"))
			utility += SALES[i];
		else if(person.getOccupation().equals("Associate professional & technician"))
			utility += 0;
		else if(person.getOccupation().equals("Armed forces"))
			utility += UNIF[i];
		else
			utility += OTHER[i];
		utility += person.hasCar()||person.hasBike()||person.hasVanBus()?VEH[i]:0;
		String pc = household.getLocation().getPostalCode();
		String pd = POSTAL_DISTRICTS.get(pc.length()==5?"0"+pc.substring(0, 1):pc.substring(0, 2));
		int pdi = new Integer(pd);
		if(1<=pdi && pdi<=4)
			utility += PC_3_4_5[i];
		else if(pdi==5 || (8<=pdi && pdi<=11))
			utility += PC_8_11[i];
		else if(18<=pdi && pdi<=23)
			utility += PC_18_22[i];
		else if(25<=pdi && pdi<=28)
			utility += 0;
		return utility;
	}

}
