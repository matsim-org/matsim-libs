package playground.dhosse.gap.scenario.mid;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.misc.Time;

import playground.dhosse.utils.EgapHashGenerator;

public class MiDSurveyPerson {

	private String id;
	private int sex;
	private int age;
	private boolean carAvailable;
	private boolean hasLicense;
	private boolean isEmployed;
	
	private Plan plan;
	
	public MiDSurveyPerson(String id, String sex, String age, String carAvailable, String hasLicense, String isEmployed){
		
		this.id = id;
		this.sex = sex.equals("male") ? 0 : 1;
		this.age = !age.equals("NULL") ? Integer.parseInt(age) : Integer.MIN_VALUE;
		
		if(carAvailable.equals("never")){
			
			this.carAvailable = false;
			
		} else{
			
			this.carAvailable = true;
			
		}
		
		this.hasLicense = !hasLicense.equals("NULL") ? Boolean.parseBoolean(hasLicense) : false;
		this.isEmployed = !isEmployed.equals("NULL") ? Boolean.parseBoolean(isEmployed) : false;
		
	}
	
	public void addPlanElement(String mode, String activityType, String distance, String startTime, String endTime){
		
		if(this.plan == null){
			
			this.plan = new PlanImpl();
			
		}
		
		if(mode.equals("car (passenger)")){
			mode = TransportMode.ride;
		}
		
		Leg leg = new LegImpl(mode);
		
		if(!startTime.equals("NULL")){
			((LegImpl)leg).setDepartureTime(Time.parseTime(startTime));
		}
		if(!endTime.equals("NULL")){
			((LegImpl)leg).setArrivalTime(Time.parseTime(startTime));
		}
		
		leg.setRoute(new GenericRouteImpl(null, null));
		double d = !distance.equals("NULL") ? Double.parseDouble(distance.replace(",", ".")) * 1000 : Double.NaN;
		leg.getRoute().setDistance(d);
		
		plan.addLeg(leg);
		
		Activity act = new ActivityImpl(activityType, new Coord(0., 0.));
		
		if(plan.getPlanElements().size() > 1){
			
			Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
			
			if(!startTime.equals("NULL")){
				lastAct.setEndTime(Time.parseTime(startTime));
			} else{
				lastAct.setEndTime(Time.UNDEFINED_TIME);
			}
			
		}
		
		if(!endTime.equals("NULL")){
			act.setStartTime(Time.parseTime(endTime));
		} else{
			act.setStartTime(Time.UNDEFINED_TIME);
		}
		
		plan.addActivity(act);
		
	}

	public String getId() {
		return id;
	}

	public int getSex() {
		return sex;
	}

	public int getAge() {
		return age;
	}

	public boolean getCarAvailable() {
		return carAvailable;
	}

	public boolean isHasLicense() {
		return hasLicense;
	}

	public boolean isEmployed() {
		return isEmployed;
	}
	
	public String generateHash(){
		
		return EgapHashGenerator.generateMiDPersonHash(this);
		
	}
	
	public Plan getPlan(){
		return this.plan;
	}
	
}
