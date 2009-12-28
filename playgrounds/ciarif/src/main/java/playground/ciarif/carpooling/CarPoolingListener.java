package playground.ciarif.carpooling;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

import playground.ciarif.retailers.stategies.GravityModelRetailerStrategy;

public class CarPoolingListener implements IterationEndsListener {
	
	public final static String CONFIG_GROUP = "carpooling";
	public final static String CONFIG_TRIPS_FILE = "workHomeTripsSummary";
	public final static String CONFIG_ANALYSIS_ITER = "TripsAnalysisIteration";				
	private Controler controler;
	private final static Logger log = Logger.getLogger(GravityModelRetailerStrategy.class);
	private WorkTrips workTrips = new WorkTrips();
	private String outputTripsFile = new String();
	private Integer tripsAnalysisIter= null;
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		this.controler = event.getControler();
		this.outputTripsFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_TRIPS_FILE);
		if (this.outputTripsFile == null) {throw new RuntimeException("In config file, param = "+CONFIG_TRIPS_FILE+" in module = "+CONFIG_GROUP+" not defined!");}
		this.tripsAnalysisIter = Integer.parseInt(controler.getConfig().findParam(CONFIG_GROUP,CONFIG_ANALYSIS_ITER));
		if (this.tripsAnalysisIter.equals(null)) {throw new RuntimeException("In config file, param = "+CONFIG_ANALYSIS_ITER+" in module = "+CONFIG_GROUP+" not defined!");}
		
		if (controler.getIteration()== this.tripsAnalysisIter) {
			this.plansAnalyzer();
		}
		
	}


	private void plansAnalyzer() {
		CarPoolingTripsWriter cptw = new CarPoolingTripsWriter(this.outputTripsFile);
		for (Person p:controler.getPopulation().getPersons().values()){
			log.info("PERSON " + p.getId() );
			Plan plan = p.getSelectedPlan();
			int tripNumber = 0;
			for (PlanElement pe:plan.getPlanElements()) {
				
				
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					
					if (act.getType().equals("home") && ((PlanImpl) plan).getNextLeg(act)!= null) {
						LegImpl homeWorkLeg = ((PlanImpl) plan).getNextLeg(act);
						ActivityImpl workAct = ((PlanImpl) plan).getNextActivity(homeWorkLeg);
						if (homeWorkLeg.getMode().toString().equals("car") && workAct.getType().contains("work")) {
							tripNumber = tripNumber+1;
							WorkTrip wt = new WorkTrip (tripNumber,(IdImpl)p.getId(), act.getCoord(),workAct.getCoord(),homeWorkLeg, true);
							this.workTrips.addTrip(wt);
						}
					}
					
					if (act.getType().contains("work") && ((PlanImpl) plan).getNextLeg(act)!=null) {
						LegImpl workHomeLeg = ((PlanImpl) plan).getNextLeg(act);
						ActivityImpl homeAct = ((PlanImpl) plan).getNextActivity(workHomeLeg);
						if (workHomeLeg.getMode().toString().equals("car") && homeAct.getType().equals("home")) {
							tripNumber = tripNumber+1;
							WorkTrip wt = new WorkTrip (tripNumber, (IdImpl)p.getId(), act.getCoord(),homeAct.getCoord(),workHomeLeg, false);
							this.workTrips.addTrip(wt);
						}	
					}
				}
			}
		}
		cptw.write(this.workTrips);
	}
}
