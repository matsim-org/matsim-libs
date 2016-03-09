package playground.dhosse.scenarios.generic.population;

import jsprit.core.util.Time;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dhosse.utils.io.AbstractCsvReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class CreatePlansFromTracks {
	
	private AbstractCsvReader reader;
	private static final String ACT_TYPE = "sighting";
	
	public static void main(String args[]){
		
		CreatePlansFromTracks cpft = new CreatePlansFromTracks();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		WKTReader wktReader = new WKTReader();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, "EPSG:32632");
		
		cpft.reader = new AbstractCsvReader(";", true) {
			
			@Override
			public void handleRow(String[] line) {
				
			String userId = line[0];
			String startedAt = line[1];
			String finishedAt = line[2];
			String geometry = line[4];
			String mode = line[5];
			
				Id<Person> personId = Id.createPersonId(userId);
				Person person = null;
				Plan plan = null;
				
				if(!scenario.getPopulation().getPersons().containsKey(personId)){
					
					person = scenario.getPopulation().getFactory().createPerson(personId);
					plan = scenario.getPopulation().getFactory().createPlan();
					
					Activity act = scenario.getPopulation().getFactory().createActivityFromCoord(
							ACT_TYPE, new Coord(0,0));
					act.setEndTime(Time.parseTimeToSeconds(startedAt.split(" ")[1]));
					plan.addActivity(act);
					
					person.addPlan(plan);
					person.setSelectedPlan(plan);
					scenario.getPopulation().addPerson(person);
					
				}
				
				Geometry g = null;
				Coord from = null;
				Coord to = null;
				
				try {
					
					g = wktReader.read(geometry);
					
				} catch (ParseException e) {
					
					e.printStackTrace();
					
				}
				
				if(g instanceof LineString){
					
					LineString ls = (LineString)g;
					from = ct.transform(MGC.coordinate2Coord(ls.getCoordinates()[0]));
					to = ct.transform(MGC.coordinate2Coord(ls.getCoordinates()[ls.getCoordinates().length-1]));
					
				} else if(g instanceof MultiLineString){
					
					MultiLineString mls = (MultiLineString)g;
					from = ct.transform(MGC.coordinate2Coord(mls.getCoordinates()[0]));
					to = ct.transform(MGC.coordinate2Coord(mls.getCoordinates()[mls.getCoordinates().length-1]));
					
				}
				
				person = scenario.getPopulation().getPersons().get(personId);
				plan = person.getSelectedPlan();
				
				if(plan.getPlanElements().size() > 1){
					
					Activity lastAct = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
					lastAct.setEndTime(Time.parseTimeToSeconds(startedAt.split(" ")[1]));
					
				} else {
					
					Activity lastAct = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
					((ActivityImpl)lastAct).setCoord(from);
					
				}
				
				Leg leg = scenario.getPopulation().getFactory().createLeg(mode.split("::")[1].toLowerCase());
				plan.addLeg(leg);
				
				Activity act = scenario.getPopulation().getFactory().createActivityFromCoord(ACT_TYPE, to);
				act.setStartTime(Time.parseTimeToSeconds(finishedAt.split(" ")[1]));
				plan.addActivity(act);
				
			}
				
		};
		
		cpft.reader.read("/home/dhosse/tracks/tracks-testwGeo.csv");
		
		new PopulationWriter(scenario.getPopulation()).write("/home/dhosse/tracks/populationFromTracks.xml.gz");
		
	}

}
