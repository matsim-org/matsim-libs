package playground.acmarmol.Avignon;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;


public class PersonPlanInfoExtractor extends AbstractPersonAlgorithm{

	private int maxS = 3; //max shopping coordinates to save
	private int maxL = 4; //max leisure coordinates to save	
	
	private Person person;
	private String score;
	private String homeCoord;
	private String workCoord;
	private String educationCoord;
	private Set<String> shopCoords = new HashSet<String>();
	private Set<String> leisureCoords = new HashSet<String>();
	private double[] distances;
	private final static DecimalFormat df = new DecimalFormat("#.##");
	
	private LinkedHashMap<Id, String> plansInfo = new LinkedHashMap<Id, String>();
	private Network network;
	final  Logger log = Logger.getLogger(PersonPlanInfoExtractor.class);
	
	
	public PersonPlanInfoExtractor() {
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}
		
	
	public void run(Scenario scenario){
		this.network = scenario.getNetwork();
		super.run(scenario.getPopulation());
	}
	
	@Override
	public void run(Person person) {
		
		startPerson(person);
		
		for (PlanElement element: this.person.getSelectedPlan().getPlanElements()){
			if (element instanceof Activity) {
				processActivity(element);
			}				
			else if (element instanceof Leg) {
				processLeg(element);
			}
		}
					
		finishPerson(person);
	}
	
	
	private void startPerson(Person person) {
		this.person = person;
		this.homeCoord = "0 , 0";
		this.workCoord = "0 , 0";
		this.educationCoord = "0 , 0";
		
		this.shopCoords.clear();
		this.leisureCoords.clear();
		this.distances = new double[MyTransportMode.MODES.size()];
		this.score = df.format(person.getSelectedPlan().getScore());
	}
	
	private void finishPerson(Person person) {
		
		StringBuilder strShop = new StringBuilder();
		strShop.append(" , ");
		strShop.append(CollectionUtils.setToString(this.shopCoords));
		
		double size = this.shopCoords.size();
		if(size==0){
			size++;
			strShop.append("0 , 0"); 
		}
		for(int i=0;i<2*(this.maxS-size);i++){
			strShop.append(" , 0");
		}
		
		
		StringBuilder strLeisure = new StringBuilder();
		strLeisure.append(" , ");
		strLeisure.append(CollectionUtils.setToString(this.leisureCoords));
		
		size = this.leisureCoords.size();
		if(size==0){
			size++;
			strLeisure.append("0 , 0"); 
		}
		for(int i=0;i<2*(this.maxL-size);i++){
			strLeisure.append(" , 0 ");
		}
					
	
		StringBuilder strDistance = new StringBuilder();
		for(double distance:distances){
			strDistance.append(df.format(distance) + " , ");
		}
		
		
		
		plansInfo.put(person.getId(),  this.score + "," + strDistance + this.homeCoord + "," + this.workCoord + "," + this.educationCoord  + strShop  + strLeisure);
		
	}


	private void processActivity(PlanElement element) {

		Activity activity = (Activity) element;
		String coord = df.format(activity.getCoord().getX()) + "," + df.format(activity.getCoord().getY());
		
		if(activity.getType().contains("h")){//home location
			this.homeCoord = coord;					
		}
		else if (activity.getType().contains("w")){//work location
			this.workCoord = coord;
			}
		else if (activity.getType().contains("e")){//education location
			this.educationCoord = coord;
			}
		else if (activity.getType().contains("s")){//shopping location
			if(this.shopCoords.isEmpty()){
				this.shopCoords.add(coord);
			}	
			else if (!this.shopCoords.contains(coord)) {
				this.shopCoords.add(coord);
				}
			}
		else if (activity.getType().contains("l")){//leisure location
			if(this.leisureCoords.isEmpty()){
				this.leisureCoords.add(coord);
			}
			else if (!this.leisureCoords.contains(coord)) {
				this.leisureCoords.add(coord);
				}
		}
		else{throw new RuntimeException("This should never happen!: activity type " + activity.getType() +" not known!");}
	}
	
	private void processLeg(PlanElement element) {
		
		Leg leg = (Leg) element;
		int index = MyTransportMode.MODES.indexOf(leg.getMode());
		if(leg.getMode().equals("car")){
		this.distances[index] += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network);
		}
		else{
		this.distances[index] += leg.getRoute().getDistance();
		}
	}
	
	public LinkedHashMap<Id, String> getPlansInfo(){
		return this.plansInfo;
	}
	
	
}
