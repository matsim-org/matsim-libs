package playground.artemc.hits;

import java.util.ArrayList;
import java.util.Date;

public class Trip {

	Date startTime = new Date();
	Date endTime  = new Date();
	Date startDate  = new Date();
	Date endDate  = new Date();
	
	String origin="";
	String actType="";
	
	double startLat=0.0;
	double startLon=0.0;
	double endLat=0.0;
	double endLon=0.0;
	
	double lastPTLat=0.0;
	double lastPTLon=0.0;
	String lastStop="";
	
	String lastPTLineInformation = "0";
	String lastPTMode = "0";
	
	boolean pt = false;
	boolean other = false;
	
	int tripID=0; 
	double tripFactor=1;
	String pax="";

	ArrayList<Stage> stages = new ArrayList<Stage>();
}
