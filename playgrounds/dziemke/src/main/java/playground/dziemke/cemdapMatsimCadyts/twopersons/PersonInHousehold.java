package playground.dziemke.cemdapMatsimCadyts.twopersons;


public class PersonInHousehold {

	private int householdId;
	private int personId;
	// private String personId;
	private int employed = 1;
	private int student = 0;
	private int driversLicence = 1;	
	private int locationOfWork;
	private int locationOfSchool = -99;
	private int female = 1;
	private int age;
	private int parent = 1;
			
	
	public PersonInHousehold(int personId, int householdId, int employed, int locationOfWork, int age) {
	// public Person(String personId, int householdId, int employed, int locationOfWork, int age) {
		this.personId = personId;
		this.householdId = householdId;
		this.employed = employed;
		this.locationOfWork = locationOfWork;
		this.age = age;
	}

	public int getHouseholdId() {
		return this.householdId;
	}

	public void setHouseholdId(int householdId) {
		this.householdId = householdId;
	}
	
	public int getPersonId() {
	// public String getpersonId() {
		return this.personId;
	}

	public void setPersonId(int personId) {
	// public void setpersonId(String personId) {
		this.personId = personId;
	}

	public int getEmployed() {
		return this.employed;
	}

	public void setEmployed(int employed) {
		this.employed = employed;
	}
	
	public int getStudent() {
		return this.student;
	}

	public void setStudent(int student) {
		this.student = student;
	}
	
	public int getDriversLicence() {
		return this.driversLicence;
	}

	public void setDriversLicence(int driversLicence) {
		this.driversLicence = driversLicence;
	}
	
	public int getLocationOfWork() {
		return this.locationOfWork;
	}

	public void setLocationOfWork(int locationOfWork) {
		this.locationOfWork = locationOfWork;
	}
	
	public int getLocationOfSchool() {
		return this.locationOfSchool;
	}

	public void setLocationOfSchool(int locationOfSchool) {
		this.locationOfSchool = locationOfSchool;
	}
	
	public int getFemale() {
		return this.female;
	}

	public void setFemale(int female) {
		this.female = female;
	}
	
	public int getAge() {
		return this.age;
	}

	public void setAge(int age) {
		this.age = age;
	}
		
	public int getParent() {
		return this.parent;
	}

	public void setParent(int parent) {
		this.parent = parent;
	}

}