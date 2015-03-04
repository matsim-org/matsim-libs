package playground.balac.avignon;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;


public class DistanceControlerListener implements StartupListener, IterationEndsListener {

	/**
	 * @param args
	 */
	double coordX = 683217.0;
	double coordY = 247300.0;
	private int numberOutside = 0;
	private int numberInside = 0;
	private static final Logger log = Logger.getLogger(DistanceControlerListener.class);
	
	//final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/Avignon/teleatlas_1pc_differentiated_1.0_fsf_dis_1/" + "Inside_outside_fsf_1.0_dis_5.txt");

	final BufferedWriter outLink = IOUtils.getBufferedWriter("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/EIRASS_2014_paper/land_price_after/" + "Inside_outside.txt");
	public DistanceControlerListener(double factor) {
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub
		numberInside = 0;
		numberOutside = 0;
		Controler controler = event.getControler();
		Scenario scenario = (ScenarioImpl) controler.getScenario();
		Population plans = scenario.getPopulation();
		//if (Integer.toString(event.getIteration()) == scenario.getConfig().getParam("controler", "lastIteration"))
		for(Person p: plans.getPersons().values()) {
			
			for(PlanElement pe: p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Activity) {
					
					if (((Activity) pe).getType().equals( "shopgrocery" )) {
						
						if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - coordX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - coordY, 2))) > 5000) {
							
							numberOutside++;
						}
						else 
							numberInside++;
						
						
					}
				}
				
				
			}
		}
		
		try {
			outLink.write(Integer.toString(numberInside) + " " + Integer.toString(numberOutside) + " " + Integer.toString(event.getIteration()));
			outLink.newLine();
			outLink.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		
		Controler controler = event.getControler();
		/*
		
		String plansFilePath = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/output/Avignon/plans0.xml";
		 
		 //String plansFilePath = "C:/Users/balacm/Desktop/plans0.xml"; 
		 //String outputFolder = "C:/Users/balacm/Desktop";
		 String outputFolder = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input";
		 
		 ObjectAttributes prefs = new ObjectAttributes();
			int counter = 0;
			int nextMsg = 1;
			for (Person p : scenario.getPopulation().getPersons().values()) {	
				for (String type : ((PersonImpl)p).getDesires().getActivityDurations().keySet()) {
					double typicalDuration = ((PersonImpl)p).getDesires().getActivityDurations().get(type);
					prefs.putAttribute(p.getId().toString(), "typicalDuration_" + type, typicalDuration);
					prefs.putAttribute(p.getId().toString(), "minimalDuration_" + type, 0.5 * 3600.0);
					prefs.putAttribute(p.getId().toString(), "earliestEndTime_" + type, 0.0 * 3600.0);
					prefs.putAttribute(p.getId().toString(), "latestStartTime_" + type, 24.0 * 3600.0);
				}
				
			}
			ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(prefs);
			betaWriter.writeFile(outputFolder + "/prefs1.xml");*/
		
		/*for (Person p : controler.getPopulation().getPersons().values()) {
		      PersonImpl pi = (PersonImpl)p;
		      Plan plan = pi.getRandomPlan();
		      for(PlanElement pe:plan.getPlanElements()) {
		    	  if (pe instanceof Activity) {
		    		  
		    		  
		    			  
		    			  double actDuration = ActivityDurationExtractor.activityDuration(((Activity) pe).getType());
		    			  if (((Activity) pe).getType().startsWith("grocery"))  {
		    			  
		    			  		pi.createDesires("grocery");
		    			  
		    			  		pi.getDesires().putActivityDuration("grocery", actDuration);
		    			  }
		    			  if (((Activity) pe).getType().startsWith("nongrocery"))  {
			    			  
		    			  		pi.createDesires("nongrocery");
		    			  
		    			  		pi.getDesires().putActivityDuration("nongrocery", actDuration);
		    			  }
		    			  else if (((Activity) pe).getType().startsWith("e"))  {
			    			  
		    			  		pi.createDesires("education");
		    			  
		    			  		pi.getDesires().putActivityDuration("education", actDuration);
		    			  }
		    			  else if (((Activity) pe).getType().startsWith("l"))  {
			    			  
		    			  		pi.createDesires("leisure");
		    			  
		    			  		pi.getDesires().putActivityDuration("leisure", actDuration);
		    			  }
		    			  else if (((Activity) pe).getType().startsWith("h"))  {
			    			  
		    			  		pi.createDesires("home");
		    			  
		    			  		pi.getDesires().putActivityDuration("home", actDuration);
		    			  }
		    			  else if (((Activity) pe).getType().startsWith("w"))  {
			    			  
		    			  		pi.createDesires("work");
		    			  
		    			  		pi.getDesires().putActivityDuration("work", actDuration);
		    			  }	
		    		  
		    	  }
		    	  
		      }
		     
		      
		    
		}
		*/
		/*Scenario scenario1 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		
		int size = scenario.getPopulation().getPersons().values().size();
		Person[] arr = (PersonImpl[]) scenario.getPopulation().getPersons().values().toArray();
		for (int i = 1; i < size; i++) {
			if (i % 10 != 0) {
				
				scenario.getPopulation().getPersons().remove(arr[i]);
			}
			
		}
		
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.writeV4("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/plans_1p.xml.gz");*/
		//populationWriter.writeV4("C:/users/balacm/Desktop/plans_1p.xml");
		
		
		int number = 0;
		int number1 = 0;
        for (ActivityFacility f: controler.getScenario().getActivityFacilities().getFacilities().values()) {
			if (f.getActivityOptions().containsKey("shopgrocery") || f.getActivityOptions().containsKey("nongrocery")) number1++;
			if (f.getActivityOptions().containsKey("shopgrocery"))
				number++;
		
		}
		log.info("Number of groceryshops " + number);
		log.info("Number of nongroceryshops " + number1);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	

}
