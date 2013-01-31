package playground.acmarmol.matsim2030.forecasts;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.Microcensus;
import playground.acmarmol.matsim2030.forecasts.timeSeriesUpdate.MicrocensusV2;
import playground.acmarmol.matsim2030.microcensus2010.MZConstants;
import playground.acmarmol.matsim2030.microcensus2010.MZPopulationUtils;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;
import playground.acmarmol.utils.MyCollectionUtils;


public class ActivityChainsAnalyzer {

	
	private final static Logger log = Logger.getLogger(ActivityChainsAnalyzer.class);
	private  Microcensus microcensus;
	private BufferedWriter out;
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String inputBase = "C:/local/marmolea/input/Activity Chains Forecast/";
		String outputBase = "C:/local/marmolea/output/Activity Chains Forecast/";
		
		String populationInputFile = inputBase + "population.15.MZ2010.xml";
		String householdInputFile = inputBase + "households.04.MZ2010.xml";
		String populationAttributesInputFile = inputBase + "populationAttributes.04.MZ2010.xml";
		String householdAttributesInputFile = inputBase + "householdAttributes.04.imputed.MZ2010.xml";
		String householdpersonsAttributesInputFile = inputBase + "householdpersonsAttributes.01.MZ2010.xml";
		Microcensus microcensus = new MicrocensusV2(populationInputFile,householdInputFile,populationAttributesInputFile,householdAttributesInputFile, householdpersonsAttributesInputFile, 2005);
		ActivityChainsAnalyzer mz2010 = new ActivityChainsAnalyzer(microcensus);

		mz2010.analyze(outputBase + "ActivityChainsAnalysisMZ2010.txt");            

	}


	public ActivityChainsAnalyzer(Microcensus microcensus){
	
		this.microcensus = microcensus;
		
	}
	
	private void analyze(String outputFile) throws IOException {
		
		out = IOUtils.getBufferedWriter(outputFile);

		
		
		printTotalChainsPerLength();
		printTotalActivitiesInChainLength(MZConstants.WORK, 1);
		//makeActivityTypeHistogram();
		
		
		out.close();
		
	}
	
	private void printTotalActivitiesInChainLength(String act_type, int i) throws IOException {
		
		double counter=0;
		
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.PERSON_WEIGHT));

			Plan plan = person.getSelectedPlan();
			int length = getChainLength(plan);
			
			if(length != i)
				continue;
			
			List<PlanElement> planElements = plan.getPlanElements();
			for (PlanElement pe : planElements) {
				if (!(pe instanceof Activity))
					continue;
				
				String type = ((Activity) pe).getType();	
				if(!type.equals(act_type))
					continue;
				
				counter+=pw;
				
			}
			
			
			
		}

		out.write("Length " + i + "\t Type " + act_type + "\t" + counter );
		
	}


	private void printTotalChainsPerLength() throws IOException{
		
		TreeMap<Integer, Double> frequencies = new TreeMap<Integer, Double>();
		
	
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), MZConstants.PERSON_WEIGHT));

			Plan plan = person.getSelectedPlan();
			int length = getChainLength(plan);
			
					
			if(!frequencies.containsKey(length)){
				frequencies.put(length, pw);
			}else{
				double total = frequencies.get(length);
				frequencies.put(length, total+pw);
			}
						
		}
		

		for(Integer length:frequencies.keySet()){
			out.write("Lenth "+ length+":\t" +frequencies.get(length));
			out.newLine();
		}
		
	}
	
	private void makeActivityTypeHistogram(){
		
		TreeMap<String, Double> frequencies = new TreeMap<String, Double>();
		
		for(Person person:microcensus.getPopulation().getPersons().values()){
			
			Plan plan = person.getSelectedPlan();
			if(plan!=null){
				List<PlanElement> planElements = plan.getPlanElements();
				for (PlanElement pe : planElements) {
					if (pe instanceof Activity){
						
					String type = ((Activity) pe).getType();	
						if(!frequencies.containsKey(type)){
							frequencies.put(type, 1.0);
						}else{
							double cum = frequencies.get(type);
							frequencies.put(type, cum+1.0);
						}
						
					}									
				}		
			}
		}
	
		
		MyCollectionUtils.printMap(frequencies);
		
	}
	
	private int getChainLength(Plan plan){
		
		int total=0;
		
		if(plan!=null){
			List<PlanElement> planElements = plan.getPlanElements();
			for (PlanElement pe : planElements) {
				if (pe instanceof Activity)
					total++;			
			}		
		}		
		return total;
	}
	
	public void workFromHomeStats(){
		
		double cum = 0;
		double sum_weight=0;
		
		for(Person person:microcensus.getPopulation().getPersons().values()){

			double pw = Double.parseDouble((String) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), "person weight"));
			Coord workCoord = (CoordImpl) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), "work: location coord");
			Coord homeCoord = (CoordImpl) microcensus.getPopulationAttributes().getAttribute((String) microcensus.getPopulationAttributes().getAttribute(person.getId().toString(), "household number"), "coord");
			
			if(homeCoord.equals(workCoord))			
				cum+= pw;
			
			sum_weight+= pw;		
		}
		
		log.info("Total working from home: " + cum/sum_weight*100 + "%");
	}

	
	
	
	
	
}



		
	

