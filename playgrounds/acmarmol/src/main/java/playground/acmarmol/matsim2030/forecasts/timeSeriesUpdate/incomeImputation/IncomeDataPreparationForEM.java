/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.incomeImputation;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.Microcensus;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;
import playground.acmarmol.utils.MyCollectionUtils;

public class IncomeDataPreparationForEM {

	private final static Logger log = Logger.getLogger(IncomeDataPreparationForEM.class);
	
	private Microcensus microcensus;
	private BufferedWriter out;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";

		String populationInputFile;
		String householdInputFile;
		String populationAttributesInputFile;
		String householdAttributesInputFile;
		Microcensus microcensus;
		
		
		populationInputFile = inputBase + "population.09.MZ2000.xml";
		householdInputFile = inputBase + "households.04.MZ2000.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.itnr.MZ2000.xml";
		//populationInputFile = inputBase + "population.03.MZ2000.xml";
		//populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2000.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2000.xml";
		microcensus = new Microcensus(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, 2000);
		IncomeDataPreparationForEM data2000 = new IncomeDataPreparationForEM(microcensus);
		
		populationInputFile = inputBase + "population.12.MZ2005.xml";
		householdInputFile = inputBase + "households.04.MZ2005.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2005.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2005.xml";
		microcensus = new Microcensus(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, 2005);
		IncomeDataPreparationForEM data2005 = new IncomeDataPreparationForEM(microcensus);
		
		
		populationInputFile = inputBase + "population.12.MZ2010.xml";
		householdInputFile = inputBase + "households.04.MZ2010.xml";
		populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2010.xml";
		householdAttributesInputFile = inputBase + "householdAttributes.04.MZ2010.xml";
		microcensus = new Microcensus(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, 2010);
		IncomeDataPreparationForEM data2010 = new IncomeDataPreparationForEM(microcensus);
		
		
		data2000.run(outputBase + "DataPreparationForEM_MZ2000.txt");
		data2005.run(outputBase + "DataPreparationForEM_MZ2005.txt");
		data2010.run(outputBase + "DataPreparationForEM_MZ2010.txt");
		
//		data2000.run(outputBase + "DataAfterImputationMZ2000.txt");
//		data2005.run(outputBase + "DataAfterImputationMZ2005.txt");
//		data2010.run(outputBase + "DataAfterImputationMZ2010.txt");
		
	}

	
	public IncomeDataPreparationForEM(Microcensus microcensus){
		this.microcensus = microcensus;
		
	}
	
	public void run(String outputFile) throws IOException{
		
		out = IOUtils.getBufferedWriter(outputFile);
		
		printTitle();
		
		for(Person person: microcensus.getPopulation().getPersons().values()){
			
			String id = person.getId().toString();
			int age = ((PersonImpl) person).getAge();
			String pw = (String) microcensus.getPopulationAttributes().getAttribute(id, MZConstants.PERSON_WEIGHT);
			String hhnr = (String) microcensus.getPopulationAttributes().getAttribute(id, MZConstants.HOUSEHOLD_NUMBER);
			String hw = (String) microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.HOUSEHOLD_WEIGHT);
			String n_car = (String) microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.TOTAL_CARS);
			
			
			int[] canton_dummy = new int[26];
			String kanton = (String) microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.CANTON);
			int canton = getCanton(kanton);
			canton_dummy[canton-1]=1;			
			
			String last_education = (String)microcensus.getPopulationAttributes().getAttribute(id, MZConstants.LAST_EDUCATION);
			double years_schooling = getYearsOfSchooling(last_education);
			String hh_income = (String)microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.HOUSEHOLD_INCOME);
			String income = getIncome(hh_income);
			String hh_size = (String)microcensus.getHouseholdAttributes().getAttribute(hhnr, MZConstants.HOUSEHOLD_SIZE);
			
			out.write(id + "\t" + pw + "\t" + hhnr + "\t" + hw + "\t" + MyCollectionUtils.integerArrayToTabSeparatedString(canton_dummy) + "\t" + age + "\t"+ Math.pow(age, 2) + "\t" + n_car + "\t" + years_schooling + "\t" + income + "\t" + hh_size); 
			out.newLine();			
		}
		
		out.close();
		
	}
	
	
	private void printTitle() throws IOException {
		out.write("id \t pw \t hhnr  \t hw \t canton01 \t canton02\tcanton03 \tcanton04 \tcanton05 \tcanton06 \tcanton07 \tcanton08 \tcanton09 \tcanton10" +
				" \tcanton11 \tcanton12 \tcanton13 \tcanton14 \tcanton15 \tcanton16 \tcanton17 \tcanton18 \tcanton19 \tcanton20\tcanton21" +
				" \tcanton22 \tcanton23 \tcanton24 \tcanton25 \tcanton26 \t age \t age2 \t nr_cars \t years_schooling  \t hh_income \t hh_size");
		out.newLine();
		
	}

	private String getIncome(String hh_income) {
		
		if(hh_income.equals("1")){
			return "1000";
		}else if(hh_income.equals("2")){
			return "3000";
		}else if(hh_income.equals("3")){
			return "5000";
		}else if(hh_income.equals("4")){
			return "7000";
		}else if(hh_income.equals("5")){
			return "9000";
		}else if(hh_income.equals("6")){
			return "11000";
		}else if(hh_income.equals("7")){
			return "13000";
		}else if(hh_income.equals("8")){
			//for MZ2000, 8 == >14000.
			//for MZ2005 and 2010, the >14000 distributes ~40% <= 16000, 60% > 16000
			//using mid-points of 15000 and 20000 (arbritary selection of +4000CHF from lower bound)
			//the weighted mid-point for 2000 is 0.4*15000 + 0.6*20000 = 18000
			
			if(this.microcensus.getYear()==2000){
				return "18000";
			}else
				return "15000";
		}else if(hh_income.equals("9")){
			return "20000";
		}else{
			return "";
		}
	}
		


	private int getCanton(String kanton) {
		
		if(kanton.equals(MZConstants.ZURICH)|| kanton.equals(MZConstants.UNSPECIFIED)){return 1;}	//unspecified = 0.3% for MZ2005, considered as canton zürich
		else if(kanton.equals(MZConstants.BERN)){return 2;}
		else if(kanton.equals(MZConstants.LUZERN)){return 3;}
		else if(kanton.equals(MZConstants.URI)){return 4;}
		else if(kanton.equals(MZConstants.SCHWYZ)){return 5;}
		else if(kanton.equals(MZConstants.OBWALDEN)){return 6;}
		else if(kanton.equals(MZConstants.NIDWALDEN)){return 7;}
		else if(kanton.equals(MZConstants.GLARUS)){return 8;}
		else if(kanton.equals(MZConstants.ZUG)){return 9;}	
		else if(kanton.equals(MZConstants.FRIBOURG)){return 10;}
		else if(kanton.equals(MZConstants.SOLOTHURN)){return 11;}
		else if(kanton.equals(MZConstants.BASEL_STADT)){return 12;}
		else if(kanton.equals(MZConstants.BASEL_LAND)){return 13;}
		else if(kanton.equals(MZConstants.SCHAFFHAUSEN)){return 14;}
		else if(kanton.equals(MZConstants.APPENZELL_AUSSERHODEN)){return 15;}
		else if(kanton.equals(MZConstants.APPENZELL_INNERHODEN)){return 16;}
		else if(kanton.equals(MZConstants.ST_GALLEN)){return 17;}	
		else if(kanton.equals(MZConstants.GRAUBUNDEN)){return 18;}
		else if(kanton.equals(MZConstants.AARGAU)){return 19;}
		else if(kanton.equals(MZConstants.THURGAU)){return 20;}
		else if(kanton.equals(MZConstants.TICINO)){return 21;}
		else if(kanton.equals(MZConstants.VAUD)){return 22;}
		else if(kanton.equals(MZConstants.VALAIS)){return 23;}
		else if(kanton.equals(MZConstants.NEUCHATEL)){return 24;}
		else if(kanton.equals(MZConstants.GENEVE)){return 25;}
		else if(kanton.equals(MZConstants.JURA)){return 26;}
		else{
			throw new RuntimeException("canton: " + kanton + " not known!");
		}
		
	}



	private double getYearsOfSchooling(String last_education) {
		

		if(last_education.equals(MZConstants.EDUCATION_NO_SCHOOL)
				|| last_education.equals(MZConstants.EDUCATION_NOT_FINISHED_MANDATORY_SCHOOL)
				|| last_education.equals(MZConstants.NO_ANSWER)
				|| last_education.equals(MZConstants.NOT_KNOWN)){
			return 0;
		}else if(last_education.equals(MZConstants.EDUCATION_MANDATORY_SCHOOL)){
			return 9;
		}else if(last_education.equals(MZConstants.EDUCATION_ONE_YEAR_AUSBILDUNG)){
			return 10;
		}else if(last_education.equals(MZConstants.EDUCATION_TWO_YEAR_BERUFLICHE_GRUNDBILDUNG)
				|| last_education.equals(MZConstants.EDUCATION_TWO_YEAR_VOLLZEITBERUFSLEHRE)){
			return 11;
		}else if(last_education.equals(MZConstants.EDUCATION_TWO_THREE_YEARS_AUSBILDUNG)){
			return 11.5;
		}else if(last_education.equals(MZConstants.EDUCATION_BERUFSLEHRE)
				|| last_education.equals(MZConstants.EDUCATION_VOLLZEITBERUFSLEHRE)
				|| last_education.equals(MZConstants.EDUCATION_MATURITAETSCHULE)){
			return 12;
		}else if(last_education.equals(MZConstants.EDUCATION_THREE_FOUR_YEARS_BERUFSLEHRE)
				|| last_education.equals(MZConstants.EDUCATION_THREE_FOUR_YEARS_VOLLZEITBERUFSLEHRE)
				|| last_education.equals(MZConstants.EDUCATION_LEHRKRAEFTE)){
			return 12.5;
		}else if(last_education.equals(MZConstants.EDUCATION_HOEHERE_BERUFSAUSBILDUNG)
				|| last_education.equals(MZConstants.EDUCATION_TECHNIKERSCHLE_HOEHEREFACHSSCHULE_FACHHOSCHSCHULE)){
			return 15.5;
		}else if(last_education.equals(MZConstants.EDUCATION_UNIVERSITAET)){
			return 16.5;
		}else{
			log.error("Last education: "+ last_education + " doesn't exist");
		}
		return 0;
	}
	
}
