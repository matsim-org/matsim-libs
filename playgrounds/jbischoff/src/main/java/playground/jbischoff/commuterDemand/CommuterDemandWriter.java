package playground.jbischoff.commuterDemand;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class CommuterDemandWriter {
	private static final Logger log = Logger.getLogger(CommuterDemandWriter.class);
	private Map<String,Geometry> municipalityMap;
	private List<CommuterDataElement> demand;
	private Scenario scenario;
	private Population population;
	private double scalefactor;
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM33N, TransformationFactory.WGS84_UTM33N);
	//adjust Coordinate System to your needs
	private double offset;
	private double start;
	private double duration;

	
	public  CommuterDemandWriter(Map<String,Geometry> municipalityMap,List<CommuterDataElement> demand){
			this.demand=demand;
			this.municipalityMap=municipalityMap;
			this.scalefactor = 1.0;
			this.offset = 4;
			this.start = 6;
			this.duration = 8.5;
	}
	
	public void writeDemand(String filename){
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(filename);
	}
	private void generatePopulation(){
		int pnr = 0;
		for (CommuterDataElement commuterDataElement : demand){
			for (int i = 0; i<commuterDataElement.getCommuters() * scalefactor; i++){
				generatePlanForZones(pnr,commuterDataElement.getFromId(),commuterDataElement.getToId());
				pnr++;
			}
			
		log.info("Created ;"+ commuterDataElement.getCommuters() + "; commuters from "+commuterDataElement.getFromName()+" ("+commuterDataElement.getFromId()+") to "+commuterDataElement.getToName()+" ("+commuterDataElement.getToId()+")");
		}
		log.info("Created "+pnr+" commuters in total" );
	
	}
		
	
	
	
	private void generatePlanForZones(int pnr, String home, String work ){
		double poffset = this.offset * 3600 * Math.random();
		double pstart = this.start * 3600 + poffset;
		double pend = (this.start+this.duration)*3600+poffset;
		
		Person p;
		p = generatePerson(home, work, pstart, pend, pnr);
		population.addPerson(p);
		
		
	}
	
	private Person generatePerson(String home, String work, double workStart, double workEnd, int pnr){
		Person p;
		Id id;
		Plan plan;
		id = scenario.createId(pnr+"_"+home.toString()+"_"+work.toString());
		p = population.getFactory().createPerson(id);
		plan = generatePlan(home,work,workStart,workEnd);
		p.addPlan(plan);
		return p;
	}
	
	
	private Plan generatePlan(String home, String work, double workStart, double workEnd){
		Plan plan= population.getFactory().createPlan(); 
		
		Coord homeCoord = this.shoot(home); 
		Coord workCoord = this.shoot(work);
	

		plan.addActivity(this.addActivity( "home", 0.0, workStart-1.0, homeCoord));
		plan.addLeg(this.addLeg(workStart -1)); 
		plan.addActivity(this.addActivity( "work",	workStart, workEnd, workCoord)); 
		plan.addLeg(this.addLeg(workEnd));
		plan.addActivity(this.addActivity( "home", workEnd + 1.0, 24.0 * 3600, homeCoord));
		return plan;
		
}
		private Activity addActivity(String type, Double start, Double end, Coord coord){
			
			Activity activity = population.getFactory().createActivityFromCoord(type, coord);
			activity.setStartTime(start);
			activity.setEndTime(end);
			return activity;
		}
			
			private Leg addLeg (double departure){ 
				Leg leg = population.getFactory().createLeg("car");
				leg.setDepartureTime(departure+1);
			return leg;
			}
			
			private Coord shoot(String home) {
				Random r = new Random();
				Point point;
				point = getRandomPointInFeature(r, this.municipalityMap.get(home));
				CoordImpl coordImpl = new CoordImpl(point.getX(), point.getY());
				return ct.transform(coordImpl);
			}

			private static Point getRandomPointInFeature(Random rnd, Geometry g) {
				Point p = null;
				double x, y;
				do {
					x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
					y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
					p = MGC.xy2Point(x, y);
				} while (!g.contains(p));
				return p;
			}

		

	public void setScalefactor(double scalefactor) {
		this.scalefactor = scalefactor;
	}

	public void setOffset(double offset) {
		this.offset = offset;
	}

	public void setStart(double start) {
		this.start = start;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	
}
