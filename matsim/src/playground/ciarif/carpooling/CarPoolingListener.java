package playground.ciarif.carpooling;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.ciarif.retailers.stategies.GravityModelRetailerStrategy;

public class CarPoolingListener implements IterationEndsListener {
	
	private Controler controler;
	private final static Logger log = Logger.getLogger(GravityModelRetailerStrategy.class);
	private WorkTrips workTrips = new WorkTrips();
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		if (controler.getIteration()%2==0 & controler.getIteration()>0) {
			this.controler = event.getControler();
			this.plansAnalyzer();
		}
		
	}


	private void plansAnalyzer() {
		// TODO Auto-generated method stub
		CarPoolingTripsWriter cptw = new CarPoolingTripsWriter("HomeWorkTrips");
		for (PersonImpl p:controler.getPopulation().getPersons().values()){
			log.info("PERSON " + p.getId() );
			PlanImpl plan = p.getSelectedPlan();
			int tripNumber = 0;
			for (PlanElement pe:plan.getPlanElements()) {
				
				
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					
					if (act.getType().equals("home") && plan.getNextLeg(act)!= null) {
						LegImpl homeWorkLeg = plan.getNextLeg(act);
						ActivityImpl workAct = plan.getNextActivity(homeWorkLeg);
						if (homeWorkLeg.getMode().toString().equals("car") && workAct.getType().equals("work")) {
							tripNumber = tripNumber+1;
							WorkTrip wt = new WorkTrip (tripNumber,(IdImpl)p.getId(), act.getCoord(),workAct.getCoord(),homeWorkLeg, true);
							this.workTrips.addTrip(wt);
						}
					}
					
					if (act.getType().equals("work") && plan.getNextLeg(act)!=null) {
						LegImpl workHomeLeg = plan.getNextLeg(act);
						ActivityImpl homeAct = plan.getNextActivity(workHomeLeg);
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
