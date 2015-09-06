package playground.southafrica.population.census2001;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.southafrica.population.census2001.containers.LivingQuarterType2001;
import playground.southafrica.population.census2001.containers.Race;
import playground.southafrica.population.census2001.containers.Schooling;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;

public class IpfWriter {
	private final static Logger LOG = Logger.getLogger(IpfWriter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(IpfWriter.class.toString(), args);
		String inputFolder = args[0];
		String outputFile = args[1];
		
		ComprehensivePopulationReader cr = new ComprehensivePopulationReader();
		cr.parse(inputFolder);
		Population population = cr.getScenario().getPopulation();
		LOG.info("Number of people in population: " + population.getPersons().size());
		ObjectAttributes personAttributes = cr.getScenario().getPopulation().getPersonAttributes();
		Households households = cr.getScenario().getHouseholds();
		ObjectAttributes householdAttributes = cr.getScenario().getHouseholds().getHouseholdAttributes();
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write(String.format("HHNR\tPNR\tHHS\tLQ\tPOP\tINC\tPNR\tAGE\tGEN\tREL\tEMPL\tSCH\n"));
			int personNumber = 1;
			for(Id<Person> personId : population.getPersons().keySet()){
				
				Id<Household> householdId = Id.create(personId.toString().split("_")[0], Household.class);

				/* Only add the person if the income class is NOT 13, i.e. unknown. */
				int incomeCode = getHouseholdIncomeCode(households.getHouseholds().get(householdId).getIncome() );
				if(incomeCode != 13){
					bw.write(householdId.toString());
					bw.write("\t");
					bw.write(String.valueOf(personNumber++));
					bw.write("\t");
					bw.write(String.valueOf(households.getHouseholds().get(householdId).getMemberIds().size()));
					bw.write("\t");
					bw.write(String.valueOf(LivingQuarterType2001.getCode((String) householdAttributes.getAttribute(householdId.toString(), "dwellingType"))));
					bw.write("\t");
					bw.write(String.valueOf(Race.getCode((String) householdAttributes.getAttribute(householdId.toString(), "population"))));
					bw.write("\t");
					bw.write(String.valueOf( incomeCode ) );
					bw.write("\t");
					bw.write(personId.toString().split("_")[1]);
					bw.write("\t");
					bw.write(String.valueOf(PersonImpl.getAge(population.getPersons().get(personId))));
					bw.write("\t");
					int gender = PersonImpl.getSex(population.getPersons().get(personId)).equalsIgnoreCase("m") ? 1 : 2;
					bw.write(String.valueOf(gender));
					bw.write("\t");
					bw.write(String.valueOf(getRelationshipCode((String) personAttributes.getAttribute(personId.toString(), "relationship"))));
					bw.write("\t");
					int employed = PersonImpl.isEmployed(population.getPersons().get(personId)) ? 1 : 0;
					bw.write(String.valueOf(employed));
					bw.write("\t");
					bw.write(String.valueOf(Schooling.getCode((String) personAttributes.getAttribute(personId.toString(), "school"))));
					bw.newLine();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter for " + outputFile);
			}
		}
		Header.printFooter();
	}

	
	public static int getHouseholdIncomeCode(Income income){
		if(income != null){
			double incomeDouble = income.getIncome();
			if(incomeDouble == 0){
				return 1;
			} else if(incomeDouble <= 7200){
				return 3;
			} else if(incomeDouble <= 26152){
				return 5;
			} else if(incomeDouble <= 108612){
				return 7;
			} else if(incomeDouble <= 434446){
				return 9;
			} else if(incomeDouble <= 1737786){
				return 11;
			} else if(incomeDouble <= 4915200){
				return 12;			
			} else {
				return 13;
			}
		} else {
			return 13;
		}
	}
	
	private static int getRelationshipCode(String type){
		if(type.equalsIgnoreCase("Head")){
			return 1;
		} else if(type.equalsIgnoreCase("Partner")){
			return 2;
		} else if(type.equalsIgnoreCase("Child")){
			return 3;
		} else if(type.equalsIgnoreCase("Sibling")){
			return 6;
		} else if(type.equalsIgnoreCase("Parent")){
			return 7;
		} else if(type.equalsIgnoreCase("Grandchild")){
			return 9;
//		} else if(type.equalsIgnoreCase("Other") ||
//				type.equalsIgnoreCase("Unrelated") ||
//				type.equalsIgnoreCase("NotApplicable") ||
//				type.equalsIgnoreCase("Unknown") ){
//			return 12;
		} else{
			return 12;
		}
	}
	

}
