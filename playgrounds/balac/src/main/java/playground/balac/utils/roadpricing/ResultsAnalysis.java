package playground.balac.utils.roadpricing;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class ResultsAnalysis {


 

public static void main(String[] args) throws IOException {


	double scorezero = 0.0;
	double income = 0.0;
	double vot = 0.0;
	Id<Person> mid;
	double scoretoll = 0.0;
	boolean crossed = false;
	boolean crossedafter = false;
	boolean usedCar = false;
	boolean usedCarafter = false;
	boolean inside = false;
	boolean insideafter = false;
	double homex = 0;
	double homey = 0;
	double workx = 0;
	double worky = 0;
	double traveltime = 0;
	double traveltimetoll = 0;
	boolean CarUser = false;
	boolean CarUserafter = false;
	String caravail;





	// args[0]= Plans file with toll
	// args[1]= Plans file without toll
	// args[X]= the desired location and name of the modified pop file .xml.gz
	// args[2]= network
	// args[3]= Attributes files with VOT



	MutableScenario scenarioZero = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	MatsimNetworkReader networkReaderZero = new MatsimNetworkReader(scenarioZero.getNetwork());
	PopulationReader populationReaderZero = new PopulationReader(scenarioZero);
	MutableScenario scenarioToll = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	MatsimNetworkReader networkReaderToll = new MatsimNetworkReader(scenarioToll.getNetwork());
	PopulationReader populationReaderToll = new PopulationReader(scenarioToll);

	final BufferedWriter agentattributes = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Downloads\\TRB/Analysis250716.txt");
	
	agentattributes.write("personID Income VOT ScoreZero ScoreToll Deltascore Crossed UsedCar Inside Crossedafter UsedCarafter Insideafter HomeX Homey Sex Age WorkX WorkY CarUser CarUserafter Caravail TravelTimeCar TravelTimeCarToll NumberCarTripsNoToll NumberCarTripsToll TotalTravelTime TotalTravelTimeToll");
	agentattributes.newLine();
	
	networkReaderZero.readFile(args[2]);
	networkReaderToll.readFile(args[2]);
	
	populationReaderZero.readFile(args[1]);
	populationReaderToll.readFile(args[0]);
	
	
	ObjectAttributes bla = new ObjectAttributes();
	
	new ObjectAttributesXmlReader(bla).readFile(args[3]);
	// args[3] is the VOT attributes file, where we want to add the deltascore	
	
	//Iterate through households and get person id's and income
	for(Person p : scenarioZero.getPopulation().getPersons().values()) {
		mid = p.getId();
		
		int age = PersonUtils.getAge(p);
		String sex = PersonUtils.getSex(p);
		caravail = PersonUtils.getCarAvail(p);
		boolean employed = PersonUtils.isEmployed(p);
		Plan plan = p.getSelectedPlan();
		scorezero = plan.getScore();
		double totaltraveltimenotoll = 0.0;
		double totaltraveltimetoll = 0.0;

	
	// go through the instances of the selected plan to check if the cordon was cross
	// and if so with which mode
		List<Activity> activities = TripStructureUtils.getActivities(plan, null);
         boolean previousInside = false;
         boolean first = true;
        

         traveltime = 0;
         
         int countcarnotoll = 0;

         for (Activity a : activities) {
        	 String type= a.getType();
        	 
        int index = plan.getPlanElements().indexOf(a);
        if (plan.getPlanElements().size() > index + 1) {
            Leg leg = (Leg) plan.getPlanElements().get(index + 1);	 
            
            totaltraveltimenotoll += leg.getTravelTime();
            String modetime = leg.getMode();
        	if (modetime.equals("car")) {
        		traveltime = traveltime + leg.getTravelTime();
        		countcarnotoll++;
        	}
        }	 
        // get work location
        if (type.contains("work")){
            workx = a.getCoord().getX();
            worky = a.getCoord().getY();
         }
         // get home location
         
         if (type.equals("home")){
             homex = a.getCoord().getX();
             homey = a.getCoord().getY();
         }
               
        if (first) {
              
               previousInside = getPosition(a);
               first = false;
               if(previousInside){
               inside = true;
               }
        }
        else {
              
               boolean currentInside = getPosition(a);
               //int index = plan.getPlanElements().indexOf(a);
               
               Leg previousLeg = (Leg) plan.getPlanElements().get(index - 1);
               String mode = previousLeg.getMode();
               if (mode.equals("car")) {
                   CarUser = true;
              }
              
               if (previousInside != currentInside) {
                    inside = false; 
                    crossed = true;
                      
                      if (mode.equals("car")) {
                           usedCar = true;
                      }
                           
               }
              
        }                   
               
    }
	            
	            
	            Person ptoll = scenarioToll.getPopulation().getPersons().get(mid);
	            Plan plantoll = ptoll.getSelectedPlan();
	            scoretoll = plantoll.getScore();
	  
	            List<Activity> activitiestoll = TripStructureUtils.getActivities(plantoll, null);
	             previousInside = false;
	             first = true;
	            
	             traveltimetoll = 0;
	             totaltraveltimetoll = 0.0;
	             int countcartoll = 0;
	             for (Activity atoll : activitiestoll) {
	             
	            	 int index = plantoll.getPlanElements().indexOf(atoll);
	            	 if (plantoll.getPlanElements().size() > index + 1) {
		                 Leg legtime = (Leg) plantoll.getPlanElements().get(index + 1);	 
		                 totaltraveltimetoll += legtime.getTravelTime();

		                 String modetime = legtime.getMode();
		                 if (modetime.equals("car")) {
		                     traveltimetoll = traveltimetoll + legtime.getTravelTime();
		                     countcartoll++;
		                 }
	            	 }
	                    if (first) {
	                          
	                           previousInside = getPosition(atoll);
	                           first = false;
	                           if(previousInside){
	                           insideafter = true;
	                           }
	                    }
	                    else {
	                    	//int index = plantoll.getPlanElements().indexOf(atoll);
	                        
	                        Leg leg = (Leg) plantoll.getPlanElements().get(index - 1);
	                        String mode = leg.getMode();
	                        if (mode.equals("car")) {
	                            CarUserafter = true;
	                       }
	                        
	                           boolean currentInside = getPosition(atoll);
	                          
	                           if (previousInside != currentInside) {
	                                insideafter = false; 
	                                crossedafter = true;
	                                  
	                                  if (mode.equals("car")) {
	                                       usedCarafter = true;
	                                  }
	                                       
	                           }
	                          
	                    }
	                    
	             }
	
	vot = (double) bla.getAttribute(mid.toString(), "traveling_car");
	income = 7948.1937*Math.pow(vot/12,1/0.1697);
	
	//The following part is used to write this value of time file into the new population file
	double deltascore = scoretoll - scorezero;
	
	
	agentattributes.write(mid + " " + income + " " + vot + " " + scorezero + " " + scoretoll + " " + deltascore + " " + crossed + " " + usedCar + " " + inside + " " + crossedafter + " " + usedCarafter + " " + insideafter + " " + homex + " " + homey + " " + sex+ " " + age+ " " + workx+ " " + worky+ " " + CarUser+ " " + CarUserafter
			+ " " + employed  + " " + caravail  + " " + traveltime + " " + countcarnotoll  + " " +  traveltimetoll + " " + countcartoll + " " + totaltraveltimenotoll + " " + totaltraveltimetoll );
	agentattributes.newLine();
	crossed = false;
	usedCar = false;
	inside = false;
	crossedafter = false;
	usedCarafter = false;
	insideafter = false;
	CarUser = false;
	CarUserafter = false;
	workx = 0;
	worky = 0;
	
	
	
	
	
	}
	
	
	
	// ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
	// betaWriter.writeFile(args[X]);
	// args[X] is the file name that we want to have published end with .xml.gz 
	
	
	}
	
	
	
	
	private static boolean getPosition(Activity a) {
	    // TODO Auto-generated method stub
		double coordx = a.getCoord().getX();
		double coordy = a.getCoord().getY();
		 
		if(Math.sqrt(Math.pow(coordx-681781.0,2)+ Math.pow(coordy-248904.0,2))<4530.0)
		    return true;
		else
			return false;
	}
}