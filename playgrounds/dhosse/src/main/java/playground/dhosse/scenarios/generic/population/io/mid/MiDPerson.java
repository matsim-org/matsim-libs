package playground.dhosse.scenarios.generic.population.io.mid;

import java.util.ArrayList;
import java.util.List;

import playground.dhosse.scenarios.generic.population.HashGenerator;

public class MiDPerson {

	private String id;
	private int sex;
	private int age;
	private boolean carAvailable;
	private boolean hasLicense;
	private boolean isEmployed;
	private double weight;
	
	private List<MiDPlan> plans;
	
	public MiDPerson(String id, String sex, String age, String carAvailable, String hasLicense, String isEmployed){
		
		this.id = id;
		this.sex = sex.equals("male") ? 0 : 1;
		this.age = !age.equals("NULL") ? Integer.parseInt(age) : Integer.MIN_VALUE;
		this.plans = new ArrayList<>();
		
		if(carAvailable.equals("never")){
			
			this.carAvailable = false;
			
		} else{
			
			this.carAvailable = true;
			
		}
		
		this.hasLicense = !hasLicense.equals("NULL") ? Boolean.parseBoolean(hasLicense) : false;
		this.isEmployed = !isEmployed.equals("NULL") ? Boolean.parseBoolean(isEmployed) : false;
		
	}
	
//	public void addPlanElement(String mode, String activityType, String distance, String startTime, String endTime){
//		
//		if(this.plan == null){
//			
//			this.plan = new PlanImpl();
//			
//		}
//		
//		if(mode.equals("car (passenger)")){
//			mode = TransportMode.ride;
//		}
//		
//		Leg leg = new LegImpl(mode);
//		
//		if(!startTime.equals("NULL")){
//			((LegImpl)leg).setDepartureTime(Time.parseTime(startTime));
//		}
//		if(!endTime.equals("NULL")){
//			((LegImpl)leg).setArrivalTime(Time.parseTime(startTime));
//		}
//		
//		leg.setRoute(new GenericRouteImpl(null, null));
//		double d = !distance.equals("NULL") ? Double.parseDouble(distance.replace(",", ".")) * 1000 : Double.NaN;
//		leg.getRoute().setDistance(d);
//		
//		plan.addLeg(leg);
//		
//		Activity act = new ActivityImpl(activityType, new Coord(0., 0.));
//		
//		if(plan.getPlanElements().size() > 1){
//			
//			Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
//			
//			if(!startTime.equals("NULL")){
//				lastAct.setEndTime(Time.parseTime(startTime));
//			} else{
//				lastAct.setEndTime(Time.UNDEFINED_TIME);
//			}
//			
//		}
//		
//		if(!endTime.equals("NULL")){
//			act.setStartTime(Time.parseTime(endTime));
//		} else{
//			act.setStartTime(Time.UNDEFINED_TIME);
//		}
//		
//		plan.addActivity(act);
//		
//	}

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
		
		return HashGenerator.generateMiDPersonHash(this);
		
	}

	public List<MiDPlan> getPlans() {
		return plans;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
}
