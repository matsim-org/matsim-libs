package playground.southafrica.population.utilities;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Households;
import org.matsim.households.Income;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.southafrica.utilities.Header;

public class WilnaIPFWriter {
	private final static Logger LOG = Logger.getLogger(WilnaIPFWriter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(WilnaIPFWriter.class.toString(), args);
		String inputFolder = args[0];
		String outputFile = args[1];
		
		Census2001SampleReader cr = new Census2001SampleReader();
		cr.parse(inputFolder);
		Population population = cr.getScenario().getPopulation();
		LOG.info("Number of people in population: " + population.getPersons().size());
		ObjectAttributes personAttributes = cr.getPersonAttributes();
		Households households = cr.getHouseholds();
		ObjectAttributes householdAttributes = cr.getHouseholdAttributes();
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write(String.format("HHNR\tHHS\tLQ\tPOP\tINC\tPNR\tAGE\tGEN\tREL\tEMPL\tSCH\n"));
			for(Id personId : population.getPersons().keySet()){
				Id householdId = new IdImpl(personId.toString().split("_")[0]);
				bw.write(householdId.toString());
				bw.write("\t");
				bw.write(String.valueOf(households.getHouseholds().get(householdId).getMemberIds().size()));
				bw.write("\t");
				bw.write(String.valueOf(getDwellingCode((String) householdAttributes.getAttribute(householdId.toString(), "dwellingType"))));
				bw.write("\t");
				bw.write(String.valueOf(getPopulationCode((String) householdAttributes.getAttribute(householdId.toString(), "population"))));
				bw.write("\t");
				bw.write(String.valueOf(getHouseholdIncomeCode(households.getHouseholds().get(householdId).getIncome() ) ) );
				bw.write("\t");
				bw.write(personId.toString().split("_")[1]);
				bw.write("\t");
				bw.write(String.valueOf(((PersonImpl) population.getPersons().get(personId)).getAge()));
				bw.write("\t");
				int gender = ((PersonImpl) population.getPersons().get(personId)).getSex().equalsIgnoreCase("m") ? 1 : 2;
				bw.write(String.valueOf(gender));
				bw.write("\t");
				bw.write(String.valueOf(getRelationshipCode((String) personAttributes.getAttribute(personId.toString(), "relationship"))));
				bw.write("\t");
				int employed = ((PersonImpl) population.getPersons().get(personId)).isEmployed() ? 1 : 0;
				bw.write(String.valueOf(employed));
				bw.write("\t");
				bw.write(String.valueOf(getSchoolCode((String) personAttributes.getAttribute(personId.toString(), "school"))));
				bw.newLine();
			}
			
		} catch (IOException e) {
			Gbl.errorMsg("Could not write to BufferedWriter " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				Gbl.errorMsg("Could not close BufferedWriter for " + outputFile);
			}
		}
		Header.printFooter();
	}

	private static int getDwellingCode(String type){
		if(type.equalsIgnoreCase("House")){
			return 1;
		} else if(type.equalsIgnoreCase("Hotel")){
			return 2;
		} else if(type.equalsIgnoreCase("StudentResidence")){
			return 3;
		} else if(type.equalsIgnoreCase("OldAgeHome")){
			return 4;
		} else if(type.equalsIgnoreCase("Hostel")){
			return 5;
		} else{
			return 6;
		}
	}

	private static int getPopulationCode(String type){
		if(type.equalsIgnoreCase("Black")){
			return 1;
		} else if(type.equalsIgnoreCase("Coloured")){
			return 2;
		} else if(type.equalsIgnoreCase("Indian-Asian")){
			return 3;
		}else if(type.equalsIgnoreCase("White")){
			return 4;
		} else{
			return 5;			
		}
	}
	
	private static int getHouseholdIncomeCode(Income income){
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
		} else{
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
	
	private static int getSchoolCode(String type){
		if(type.equalsIgnoreCase("None")){
			return 1;
		} else if(type.equalsIgnoreCase("PreSchool")){
			return 2;
		} else if(type.equalsIgnoreCase("School")){
			return 3;
		} else if(type.equalsIgnoreCase("Tertiary")){
			return 6;
		} else if(type.equalsIgnoreCase("AdultEducation")){
			return 7;
		} else{
			return 8;
		}
	}



}
