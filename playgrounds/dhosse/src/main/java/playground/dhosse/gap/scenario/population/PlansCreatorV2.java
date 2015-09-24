package playground.dhosse.gap.scenario.population;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.matrices.Matrix;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;
import playground.dhosse.gap.scenario.mid.MiDCSVReader;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupData;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupTemplates;
import playground.dhosse.gap.scenario.mid.MiDSurveyPerson;
import playground.dhosse.gap.scenario.population.io.CommuterDataElement;
import playground.dhosse.gap.scenario.population.io.CommuterFileReader;
import playground.dhosse.gap.scenario.population.personGroups.CreateCommutersFromElsewhere;
import playground.dhosse.gap.scenario.population.personGroups.CreateDemand;
import playground.dhosse.utils.EgapHashGenerator;

public class PlansCreatorV2 {
	
	private static final Logger log = Logger.getLogger(PlansCreatorV2.class);
	
	public static Population createPlans(Scenario scenario, String commuterFilename, String reverseCommuterFilename, Map<String,Matrix> matrices){
		
		//create a commuter reader and add municipalities to the filter in order to create commuter relations
		//that start or end in these municipalities
		CommuterFileReader cdr = new CommuterFileReader();
		
		cdr.addFilter("09180"); //GaPa (Kreis)
		cdr.addFilter("09180113"); //Bad Bayersoien
		cdr.addFilter("09180112"); //Bad Kohlgrub
		cdr.addFilter("09180114"); //Eschenlohe
		cdr.addFilter("09180115"); //Ettal
		cdr.addFilter("09180116"); //Farchant
		cdr.addFilter("09180117"); //Garmisch-Partenkirchen
		cdr.addFilter("09180118"); //Grainau
		cdr.addFilter("09180119"); //Großweil
		cdr.addFilter("09180122"); //Krün
		cdr.addFilter("09180123"); //Mittenwald
		cdr.addFilter("09180124"); //Murnau a Staffelsee
		cdr.addFilter("09180125"); //Oberammergau
		cdr.addFilter("09180126"); //Oberau
		cdr.addFilter("09180127"); //Ohlstadt
		cdr.addFilter("09180128"); //Riegsee
		cdr.addFilter("09180129"); //Saulgrub
		cdr.addFilter("09180131"); //Schwaigen
		cdr.addFilter("09180132"); //Seehausen a Staffelsee
		cdr.addFilter("09180134"); //Uffind a Staffelsee
		cdr.addFilter("09180135"); //Unterammergau
		cdr.addFilter("09180136"); //Wallgau
		
		cdr.read(reverseCommuterFilename, true);
		cdr.read(commuterFilename, false);
		
		//the actual demand generation
		createPersonsWithDemographicData(scenario, cdr.getCommuterRelations(), matrices);
		
		return scenario.getPopulation();
		
	}
	
	private static void createPersonsWithDemographicData(Scenario scenario, Map<String, CommuterDataElement> relations, Map<String,Matrix> matrices){
		
		Map<String,MiDPersonGroupData> personGroupData = EgapPopulationUtilsV2.createMiDPersonGroups();
		
		MiDPersonGroupTemplates templates = new MiDPersonGroupTemplates();
		
		MiDCSVReader reader = new MiDCSVReader();
		reader.readV2(Global.matsimInputDir + "MID_Daten_mit_Wegeketten/travelsurvey_m.csv", templates);
		templates.setWeights();
		
		
//		for(MiDSurveyPerson p : persons.values()){
//			templates.handlePerson(p);
//		}
		
		for(Entry<String, Municipality> entry : Municipalities.getMunicipalities().entrySet()){
			
			CreateDemand.runTryout(entry.getKey(), 6, 17, entry.getValue().getnStudents(), scenario, templates, matrices);
//			createPersonsFromPersonGroup(entry.getKey(), 6, 17, entry.getValue().getnStudents(), scenario, personGroupData.get("0_17"));
			
			int nCommuters = 0;
			List<String> keysToRemove = new ArrayList<>();
			
			for(String relation : relations.keySet()){
				
				String[] relationParts = relation.split("_");
				
				if(relationParts[0].startsWith(entry.getKey())){
	
					nCommuters += relations.get(relation).getCommuters();
					
					if(relationParts[1].startsWith("09180")){
						
						CreateDemand.createCommuters(relationParts[0], relationParts[1], 18, 65, relations.get(relation), scenario, templates, matrices);
//						createCommutersFromKey(scenario, relations.get(relation), personGroupData, templates);
						keysToRemove.add(relation);
						
					}
					
				}
				
			}
			
			for(String s : keysToRemove){
				
				relations.remove(s);
				
			}
			
			CreateDemand.runTryout(entry.getKey(), 18, 65, entry.getValue().getnAdults() - nCommuters, scenario, templates, matrices);
			CreateDemand.runTryout(entry.getKey(), 66, 100, entry.getValue().getnPensioners(), scenario, templates, matrices);
			
		}
		
		CreateCommutersFromElsewhere.run(scenario.getPopulation(), relations.values(), personGroupData);
		
	}
	
	public static double createRandomTimeShift(double variance){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = Global.random.nextDouble();
		double r2 = Global.random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = variance * 3600 * normal;
		
		return endTime;
		
	}
	
}