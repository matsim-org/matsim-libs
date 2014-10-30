package playground.dziemke.cemdapMatsimCadyts.oneperson;


public class SimplePerson {

	private int householdId;
	// private int personId;
	private String personId;
	//private int employed = 1;
	private int employed;
	//private int student;
	private int student = 0;
	private int driversLicence = 1;	
	private int locationOfWork;
	//private int locationOfSchool = -99;
	private int locationOfSchool;
	//private int female = 1;
	private int sex;
	private int age;
	//private int parent = 1;
	private int parent = 0;
			
	
	// public Person(int personId, int householdId, int locationOfWork, int age) {
	public SimplePerson(String personId, int householdId, int employed, int student, int locationOfWork, int locationOfSchool, int sex, int age) {
		this.personId = personId;
		this.householdId = householdId;
		this.employed = employed;
		this.student = student;
		this.locationOfWork = locationOfWork;
		this.locationOfSchool = locationOfSchool;
		this.age = age;
		this.sex = sex;
	}

	public int getHouseholdId() {
		return this.householdId;
	}

	public void setHouseholdId(int householdId) {
		this.householdId = householdId;
	}
	
	// public int getpersonId() {
	public String getpersonId() {
		return this.personId;
	}

	// public void setpersonId(int personId) {
	public void setpersonId(String personId) {
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
	
	public int getSex() {
		return this.sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
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