package playground.southafrica.population.census2011;

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
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.southafrica.population.census2011.containers.HousingType2011;
import playground.southafrica.population.census2011.containers.Income2011;
import playground.southafrica.population.census2011.containers.MainDwellingType2011;
import playground.southafrica.population.census2011.containers.PopulationGroup2011;
import playground.southafrica.population.census2011.containers.Relationship2011;
import playground.southafrica.population.census2011.containers.School2011;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;

public class IpfWriter2011 {
	private final static Logger LOG = Logger.getLogger(IpfWriter2011.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(IpfWriter2011.class.toString(), args);
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
			bw.write(String.format("HHNR\tPNR\tHHS\tHT\tMDT\tPOP\tINC\tPNRHH\tAGE\tGEN\tREL\tEMPL\tSCH\n"));
			int personNumber = 1;
			for(Id<Person> personId : population.getPersons().keySet()){
				
				Id<Household> householdId = Id.create(personId.toString().split("_")[0], Household.class);

				/* Only add the person if the household income class is known. */
				Income2011 income = Income2011.getIncomeEnum(households.getHouseholds().get(householdId).getIncome());

				if(income != Income2011.Unspecified && income != Income2011.NotApplicable){
					int incomeCode = Income2011.getCode(income);
					
					/* Household id. */
					bw.write(householdId.toString());
					bw.write("\t");
					/* Unique person number. */
					bw.write(String.valueOf(personNumber++));
					bw.write("\t");
					/* Household size. */
					bw.write(String.valueOf(households.getHouseholds().get(householdId).getMemberIds().size()));
					bw.write("\t");
					/* Household type. */
					bw.write(String.valueOf(HousingType2011.getCode((String) householdAttributes.getAttribute(householdId.toString(), "housingType"))));
					bw.write("\t");
					/* Main dwelling type. */
					bw.write(String.valueOf(MainDwellingType2011.getCode((String) householdAttributes.getAttribute(householdId.toString(), "mainDwellingType"))));
					bw.write("\t");
					/* Population group. */
					bw.write(String.valueOf(PopulationGroup2011.getCode((String) householdAttributes.getAttribute(householdId.toString(), "population"))));
					bw.write("\t");
					/* Income code. */
					bw.write(String.valueOf( incomeCode ) );
					bw.write("\t");
					/* Number of person IN the household. */
					bw.write(personId.toString().split("_")[1]);
					bw.write("\t");
					/* Age. */
					bw.write(String.valueOf(PersonImpl.getAge(population.getPersons().get(personId))));
					bw.write("\t");
					/* Gender. */
					int gender = PersonImpl.getSex(population.getPersons().get(personId)).equalsIgnoreCase("m") ? 1 : 2;
					bw.write(String.valueOf(gender));
					bw.write("\t");
					/* Relation/role in household. */
					Relationship2011 relationship = Relationship2011.parseRelationshipFromString((String) personAttributes.getAttribute(personId.toString(), "relationship"));
					bw.write(String.valueOf(Relationship2011.getCode(relationship)));
					bw.write("\t");
					/* Employment status */
					int employed = PersonImpl.isEmployed(population.getPersons().get(personId)) ? 1 : 0;
					bw.write(String.valueOf(employed));
					bw.write("\t");
					/* Level of school currently attending. */
					School2011 school = School2011.parseEducationFromString((String) personAttributes.getAttribute(personId.toString(), "school"));
					bw.write(String.valueOf(School2011.getCode(school)));
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

}
