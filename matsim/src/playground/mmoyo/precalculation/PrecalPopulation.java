package playground.mmoyo.precalculation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.core.network.NetworkLayer;

public class PrecalPopulation {
	
	private TransitSchedule transitSchedule;
	private NetworkLayer plainNetwork;
	private String planFile;
	private String configFile;
	
	public PrecalPopulation(final TransitSchedule transitSchedule, final NetworkLayer plainNetwork, final String planFile, final String configFile ) {
		this.transitSchedule = transitSchedule;
		this.plainNetwork = plainNetwork;
		this.planFile = planFile; 
		this.configFile = configFile;
	}

	public void PreCal(){
		PrecalRoutes precalRoutes= new PrecalRoutes(transitSchedule, plainNetwork);
		PopulationImpl population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, plainNetwork);
		plansReader.readFile(planFile);
		//Config config = new Config();
		//config = Gbl.createConfig(new String[]{ configFile, "http://www.matsim.org/files/dtd/plans_v4.dtd"});
		
		long startTime = System.currentTimeMillis();
		int beneficiados=0;
		for (PersonImpl person: population.getPersons().values()) {
		//if ( true ) {
			//PersonImpl person = population.getPersons().get(new IdImpl("35420")); // 5636428  2949483 
			PlanImpl plan = person.getPlans().get(0);

			int foundConns=0;
			boolean first =true;
			
			ActivityImpl lastAct = null;       
			ActivityImpl thisAct= null;		 
				
			for (PlanElement pe : plan.getPlanElements()) {   		//temporarily commented in order to find only the first leg
				//for	(int elemIndex=0; elemIndex<3; elemIndex++){            //  finds only
				//PlanElement pe= plan.getPlanElements().get(elemIndex);  //  the first trip
				
				if (pe instanceof ActivityImpl) {  				
					thisAct= (ActivityImpl) pe;					
					if (!first) {								
						Coord lastActCoord = lastAct.getCoord();
			    		Coord actCoord = thisAct.getCoord();
			    		foundConns = precalRoutes.findPTPath(lastActCoord, actCoord, 400);
					}
		    		lastAct = thisAct;
		    		first=false;
				}
			}
			if (foundConns>0) ++beneficiados;
			System.out.println (person.getId());
		}//for person
		double duracion= System.currentTimeMillis()-startTime;
		int intDuracion= (int)duracion;
		System.out.print("duracion total:  ") ;
		System.out.print(intDuracion);
		System.out.println("agentes: " + population.getPersons().values().size());
		System.out.println ("beneficiados:" + beneficiados);
		
	}
	
	
	
}
