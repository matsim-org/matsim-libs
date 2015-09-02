package saleem.stockholmscenario.teleportation;

public class Stop {//To represent a stop which will be written to ptStops.csv
	String id,x,y;
	Stop(String id, String x, String y){
		this.id=id;
		this.x=x;
		this.y=y;
	}
	public String getX(){
		return this.x;
	}
	public String getY(){
		return this.y;
	}
	public String getID(){
		return this.id;
	}
}
