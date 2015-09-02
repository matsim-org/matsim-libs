package saleem.stockholmscenario.stockholmptdata;

public class VehicleType {
public String id;
public String desciption;
public int numberofseats;
public int standingcapacity;
public double length = 12;
public double width = 2.5;
public double accesstime = 1.0;
public double egresstime = 1.0;
public String dooroperationmode = "serial";
public double passengercarequivalents = 1.0;

public void setLength(double length ){
	this.length=length;
}
public void setWidth(double width ){
	this.width=width;
}
public void setID(String id ){
	this.id=id;
}
public void setDesciption(String desciption ){
	this.desciption=desciption;
}
public void setNumberOfSeats(int numberofseats ){
	this.numberofseats=numberofseats;
}
public void setStandingCapacity(int standingcapacity ){
	this.standingcapacity=standingcapacity;
}

public double getLength( ){
	return this.length;
}
public String getDoorOperationMode( ){
	return this.dooroperationmode;
}
public double getPassengerCarEquivalents( ){
	return this.passengercarequivalents;
}
public double getWidth(){
	return this.width;
}
public double getAccessTime(){
	return this.accesstime;
}
public double getEgressTime(){
	return this.egresstime;
}
public String getID(){
	return this.id;
}
public String getDesciption(){
	return this.desciption;
}
public int getNumberOfSeats(){
	return this.numberofseats;
}
public int getStandingCapacity(){
	return this.standingcapacity;
}
}
