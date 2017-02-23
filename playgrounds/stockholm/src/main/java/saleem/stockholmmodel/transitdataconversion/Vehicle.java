package saleem.stockholmmodel.transitdataconversion;

/**
 * Class specifying a vehicle instance within transit schedule
 * 
 * 
 * @author Mohammad Saleem
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
