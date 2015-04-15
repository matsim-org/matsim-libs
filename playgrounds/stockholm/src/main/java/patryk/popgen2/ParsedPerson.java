package patryk.popgen2;

public class ParsedPerson {
	
	private String id;
	private String homeZone;
	private String workZone;
	private String mode;
	private int age;
	private int income;
	private int sex;
	private int housingType;
	
	public ParsedPerson (String id, String homeZone, String workZone, String mode,
			int age, int income, int sex, int housingType) {
		
			this.id = id;
			this.homeZone = homeZone;
			this.workZone = workZone;
			this.mode = mode;
			this.age = age;
			this.income = income;
			this.sex = sex;
			this.housingType = housingType;
	}	
	
	public String getId() {
		return id;
	}
	
	public String getHomeZone() {
		return homeZone;
	}
	
	public String getWorkZone() {
		return workZone;
	}
	
	public String getMode() {
		return mode;
	}
	
	public int getAge() {
		return age;
	}
	
	public int getIncome() {
		return income;
	}
	
	public int getSex() {
		return sex;
	}
	
	public int getHousingType() {
		return housingType;
	}
}
