package playground.dhosse.scenarios.generic.population.io.mid;

public class MiDActivity implements MiDPlanElement {

	private int id = 0;
	
	private double startTime;
	private double endTime;
	private String actType;
	
	public MiDActivity(String actType){
		this.actType = actType;
	}
	
	@Override
	public int getId() {
		return this.id;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public String getActType() {
		return actType;
	}

	public void setActType(String actType) {
		this.actType = actType;
	}

}
