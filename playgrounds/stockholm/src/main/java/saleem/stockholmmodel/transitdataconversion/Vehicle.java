package saleem.stockholmmodel.transitdataconversion;

/**
 * Class representing a vehicle instance within transit schedule
 */
public class Vehicle {
	private String id;
	private String type;
	public void setID(String id ){
		this.id=id;
	}
	public String getID(){
		return this.id;
	}
	public void setType(String type ){
		this.type=type;
	}
	public String getType(){
		return this.type;
	}
}
