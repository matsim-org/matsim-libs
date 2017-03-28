package playground.santiago.utils;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;


public class GetUtilInfo {

	private final static Logger log = Logger.getLogger(GetUtilInfo.class);

	final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";
	final String originalPlans = svnWorkingDir + "inputForMATSim/plans/1_initial/workDaysOnly/plans_final.xml.gz";
	
	final String databaseFilesDir = svnWorkingDir + "inputFromElsewhere/exportedFilesFromDatabase/";
	final String Normal = databaseFilesDir + "Normal/";
	final String personasFile =  Normal + "Persona.csv";
	
	final double percentage = 0.1;
	
	final String outputFolder = svnWorkingDir + "inputForMATSim/plans/2_10pct/";
	
	private Map<String,Double> idsFactorsSantiago;
	private int totalPopulation;
	private Population originalPopulation;
	private Map<String,Double> idsFactorsMatsim;
	private double proportionalFactor;
	
	public static void main(String[] args) {

		GetUtilInfo gui = new GetUtilInfo();
		gui.writeAll();

	}
	private void getIdsAndFactorsSantiago(){

		this.idsFactorsSantiago = new TreeMap<String,Double>();

			try {
					
				BufferedReader bufferedReader = IOUtils.getBufferedReader(personasFile);				
				String currentLine = bufferedReader.readLine();				
					while ((currentLine = bufferedReader.readLine()) != null) {
						String[] entries = currentLine.split(",");
						idsFactorsSantiago.put(entries[1], Double.parseDouble(entries[33]));
							
					}

				bufferedReader.close();
					
				} catch (IOException e) {
					
					log.error(new Exception(e));
				
				}


	}
	private void getTotalPopulationSantiago(){

		double population=0;

		for (Map.Entry<String, Double> entry : idsFactorsSantiago.entrySet()){
			population += entry.getValue();
		}

		this.totalPopulation = (int)Math.round(population);
		System.out.println("The total number of persons in Santiago is " + totalPopulation + ". Obs: this number differs from the total population stored in SantiagoScenarioConstants.java by 35 persons... ");

	}
	
	private void getIdsAndFactorsMatsimPop(){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);
		pr.readFile(originalPlans);
		this.originalPopulation = scenario.getPopulation();
		
		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());		

		List<String> IdsMatsim = new ArrayList<>();
		

		for (Person p : persons){			
			IdsMatsim.add(p.getId().toString());	
		}
		
		this.idsFactorsMatsim = new TreeMap <String,Double>();
		
		for(String Ids : IdsMatsim ) {
			idsFactorsMatsim.put(Ids, idsFactorsSantiago.get(Ids));		
		}



	
	}
	private void getProportionalFactor(){
		

		double sumFactors = 0;
		
		for (Map.Entry<String,Double> entry : idsFactorsMatsim.entrySet()){		
			sumFactors += entry.getValue();
		}

		this.proportionalFactor = (percentage*totalPopulation)/sumFactors;
		System.out.println("The proportional factor is: " + proportionalFactor);

		}

	private void writeAll(){
		getIdsAndFactorsSantiago();
		getTotalPopulationSantiago();
		getIdsAndFactorsMatsimPop();
		getProportionalFactor();

		List<Person> persons = new ArrayList<>(originalPopulation.getPersons().values());


		try{
			PrintWriter pw = new PrintWriter (new FileWriter ( outputFolder + "factorsByAgent.txt" ));
			pw.println("Agent ID" + "\t" + "ODS Factor" + "\t" + "Cloning Factor");

			for (Person p : persons) {
				String keyId = p.getId().toString();
				int clonateFactor = (int)Math.round(proportionalFactor*idsFactorsMatsim.get(keyId));
				pw.println(keyId + "\t" + idsFactorsMatsim.get(keyId) + "\t" + clonateFactor);
			}
			pw.close();
			
		}catch(IOException e){
			log.error(new Exception(e));
		}
	}

}
