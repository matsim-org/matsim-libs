package playground.balmermi.toggenburg.modules;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;

public class PopulationAnalysis {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PopulationAnalysis.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PopulationAnalysis() {
		log.info("init " + this.getClass().getName() + " module...");
		log.info("done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final void populationStats(PopulationImpl population, Set<IdImpl> toggenLinks) {
		// Geschlechter Verteilung (Anzahl), Altersverteilung, Fahrausweis, Autoverf�gbarkeit, Berufst�tig, �V Abo Verf�gbarkeit
		// index  : 0..99  100    101  102  103          104           105          106       107       108    109     110     111
		// meaning: 0..99  >=100  m    f    licenseTrue  licenseFalse  availAlways  availSom  availNev  eTrue  eFalse  ptTrue  ptFalse
		int[] stats = new int[112];
		int count = 0;
		for (int i=0; i<stats.length; i++) { stats[i] = 0; }
		for (Person pp : population.getPersons().values()) {
			PersonImpl p = (PersonImpl) pp;
			boolean analyse = true;
			List<PlanElement> e = p.getSelectedPlan().getPlanElements();
			for (int i=0; i<e.size(); i++) {
				if (i%2==0) {
					ActivityImpl a = (ActivityImpl)e.get(i);
					if (toggenLinks.contains(a.getLinkId())) {
						analyse = true;
						break;
					}
				}
				else {
					LegImpl l = (LegImpl)e.get(i);
					if (l.getRoute() instanceof NetworkRouteWRefs) {
						NetworkRouteWRefs ll = (NetworkRouteWRefs)l.getRoute();
						for (Id lid : ll.getLinkIds()) {
							if (toggenLinks.contains(lid)) {
								analyse = true;
								break;
							}
						}
					}
				}
			}
			if (analyse) {
				int age = p.getAge();
				if (age > 99) { stats[100]++; } else { stats[age]++; }
				if (p.getSex().equals("m")) { stats[101]++; } else { stats[102]++; }
				if (p.hasLicense()) { stats[103]++; } else { stats[104]++; }
				if (p.getCarAvail().startsWith("a")) { stats[105]++; }
				else if (p.getCarAvail().startsWith("s")) { stats[106]++; }
				else { stats[107]++; }
				if (p.isEmployed()) { stats[108]++; } else { stats[109]++; }
				if (p.getTravelcards() == null) { stats[111]++; }
				else if (p.getTravelcards().isEmpty()) { stats[111]++; }
				else { stats[110]++; }
				count++;
			}
		}
		for (int i=0; i<stats.length; i++) {
			System.out.println(i+":\t"+stats[i]);
		}
		System.out.println(count+" out of "+population.getPersons().size()+" analyzed.");
	}
	
	private final void tripStats(PopulationImpl population) {
		// \    h  w  e  s  l
		// car
		// pt
		// bike
		// walk
		// ride
		double[][] dist = new double[5][5];
		int[][] count = new int[5][5];
		for (int i=0; i<5; i++) {
			for (int j=0; j<5; j++) {
				dist[i][j] = 0.0;
				count[i][j] = 0;
			}
		}
		for (Person p : population.getPersons().values()) {
			List<PlanElement> e = p.getSelectedPlan().getPlanElements();
			for (int i=2; i<e.size(); i=i+2) {
				ActivityImpl a = (ActivityImpl)e.get(i);
				LegImpl l = (LegImpl)e.get(i-1);
				int col = -1;
				int row = -1;
				if (a.getType().startsWith("h")) { col = 0; }
				else if (a.getType().startsWith("w")) { col = 1; }
				else if (a.getType().startsWith("e")) { col = 2; }
				else if (a.getType().startsWith("s")) { col = 3; }
				else if (a.getType().startsWith("l")) { col = 4; }
				
				if (l.getMode() == TransportMode.car) { row = 0; }
				else if (l.getMode() == TransportMode.pt) { row = 1; }
				else if (l.getMode() == TransportMode.bike) { row = 2; }
				else if (l.getMode() == TransportMode.walk) { row = 3; }
				else if (l.getMode() == TransportMode.ride) { row = 4; }
				
				dist[row][col] += l.getRoute().getDistance();
				count[row][col]++;
			}
		}
		for (int i=0; i<5; i++) {
			for (int j=0; j<5; j++) {
				System.out.println("["+i+","+j+"]"+":\t"+dist[i][j]+"\t"+count[i][j]);
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////
	
	public void run(PopulationImpl population, Set<IdImpl> toggenLinks) {
		log.info("running " + this.getClass().getName() + " module...");
		this.populationStats(population,toggenLinks);
		this.tripStats(population);
		log.info("done.");
	}
}
