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
	private int personGroup;
	private int lifePhase;
	
	private List<MiDPlan> plans;
	
	public MiDPerson(String id, String sex, String age, String carAvailable, String hasLicense, String isEmployed){
		
		this.id = id;
		this.sex = sex.equals(MiDConstants.SEX_MALE) ? 0 : 1;
		this.age = !age.equals(MiDConstants.NAN) ? Integer.parseInt(age) : Integer.MIN_VALUE;
		
		if(carAvailable.equals("1") || carAvailable.equals("2")){
			
			this.carAvailable = true;
			
		} else{
			
			this.carAvailable = false;
			
		}
		
		this.hasLicense = hasLicense.equals("1") ? true : false;
		this.isEmployed = isEmployed.equals("1") ? true : false;
		
		this.plans = new ArrayList<>();
		
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

	public int getPersonGroup() {
		return personGroup;
	}

	public void setPersonGroup(int personGroup) {
		this.personGroup = personGroup;
	}

	public int getLifePhase() {
		return lifePhase;
	}

	public void setLifePhase(int lifePhase) {
		this.lifePhase = lifePhase;
	}
	
}
