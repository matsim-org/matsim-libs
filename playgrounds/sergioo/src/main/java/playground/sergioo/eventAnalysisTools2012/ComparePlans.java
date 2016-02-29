package playground.sergioo.eventAnalysisTools2012;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

public class ComparePlans {

	//Constants
	private static final String SEPARATOR_TXT = "\t";
	
	//Constructors
	public ComparePlans(String networkFile, String plansFile, String outFile) throws Exception {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario.getNetwork()).parse(networkFile);
		new MatsimPopulationReader(scenario).parse(plansFile);
		writeModeChoice(scenario.getPopulation(), outFile, scenario.getNetwork());
	}
	
	//Methods
	private void writeModeChoice(Population population, String outFile, Network network) throws Exception {
		PrintWriter writer = new PrintWriter(outFile);
		String s = SEPARATOR_TXT;
		writer.println(""+s+""+s+"car"+s+""+s+""+s+"pt");
		writer.println("id"+s+"trip"+s+"TT"+s+"distance"+s+"ERP"+s+"TT"+s+"fare"+s+"transfers"+s+"walk");
		for(Person person:population.getPersons().values())
			for(int p1=0; p1<person.getPlans().size(); p1++)
				for(int p2=p1+1; p2<person.getPlans().size(); p2++)
					writeScoreDetails(writer, person.getId(), person.getPlans().get(p1), person.getPlans().get(p2), network);
		writer.close();
	}
	private void writeScoreDetails(PrintWriter writer, Id<Person> personId, Plan plan1, Plan plan2, Network network) throws Exception { 
		String s = SEPARATOR_TXT;
		List<Tuple<String, Coord>> activities1 = new ArrayList<Tuple<String, Coord>>();
		for(PlanElement planElement:plan1.getPlanElements())
			if(planElement instanceof Activity && !((Activity)planElement).getType().equals("pt interaction"))
				activities1.add(new Tuple<String, Coord>(((Activity)planElement).getType(), ((Activity)planElement).getCoord()));
		List<Tuple<String, Coord>> activities2 = new ArrayList<Tuple<String, Coord>>();
		for(PlanElement planElement:plan2.getPlanElements())
			if(planElement instanceof Activity && !((Activity)planElement).getType().equals("pt interaction"))
				activities2.add(new Tuple<String, Coord>(((Activity)planElement).getType(), ((Activity)planElement).getCoord()));
		boolean sameChain = true;
		if(activities1.size() != activities2.size())
			sameChain = false;
		else
			for(int a=0; a<activities1.size(); a++)
				if(!(activities1.get(a).getFirst().equals(activities2.get(a).getFirst()) && activities1.get(a).getSecond().equals(activities2.get(a).getSecond())))
					sameChain = false;
		if(sameChain) {
			int differentMode = 0;
			int p1=1, p2=1, l=0;
			boolean inActivity1=false, inActivity2=false;
			for(;p1<plan1.getPlanElements().size() && p2<plan2.getPlanElements().size(); p1++, p2++) {
				PlanElement planElement1 = plan1.getPlanElements().get(p1), planElement2 = plan2.getPlanElements().get(p2);
				if(planElement1 instanceof Leg && planElement2 instanceof Leg) {
					if(((Leg)planElement1).getMode().equals("car") && ((Leg)planElement2).getMode().equals("transit_walk")) {
						String line = personId+s+l+s+((Leg)planElement1).getTravelTime()+s+RouteUtils.calcDistanceExcludingStartEndLink(((NetworkRoute)((Leg)planElement1).getRoute()), network);
						double time=0, distance=0, fare=0, numTransfers=0, walk=0;
						while(!(planElement2 instanceof Activity) || ((Activity)planElement2).getType().equals("pt interaction")) {
							if(planElement2 instanceof Leg) {
								time+=((Leg)planElement2).getTravelTime();
								if(((Leg)planElement2).getMode().equals("transit_walk"))
									walk+=((Leg)planElement2).getTravelTime();
								else
									numTransfers++;
							}
							p2++;
							planElement2 = plan2.getPlanElements().get(p2);
						}
						line+=time+s+fare+s+numTransfers+s+walk;
						writer.println(line);
					}
					else if(((Leg)planElement2).getMode().equals("car") && ((Leg)planElement1).getMode().equals("transit_walk")) {
						String line = personId+s+l+s+((Leg)planElement2).getTravelTime()+s+RouteUtils.calcDistanceExcludingStartEndLink(((NetworkRoute)((Leg)planElement2).getRoute()), network);
						double time=0, distance=0, fare=0, numTransfers=0, walk=0;
						while(!(planElement1 instanceof Activity) || ((Activity)planElement1).getType().equals("pt interaction")) {
							if(planElement1 instanceof Leg) {
								time+=((Leg)planElement1).getTravelTime();
								if(((Leg)planElement1).getMode().equals("transit_walk"))
									walk+=((Leg)planElement1).getTravelTime();
								else
									numTransfers++;
							}
							p1++;
							planElement1 = plan1.getPlanElements().get(p1);
						}
						line+=time+s+fare+s+numTransfers+s+walk;
						writer.println(line);
					}
					else if(((Leg)planElement1).getMode().equals("car") && ((Leg)planElement2).getMode().equals("car")) {
						p1++;
						p2++;
					}
					else {
						while(!(planElement1 instanceof Activity) || ((Activity)planElement1).getType().equals("pt interaction")) {
							p1++;
							planElement1 = plan1.getPlanElements().get(p1);
						}
						while(!(planElement2 instanceof Activity) || ((Activity)planElement2).getType().equals("pt interaction")) {
							p2++;
							planElement2 = plan2.getPlanElements().get(p2);
						}
					}
					l++;
				}
				else
					throw new Exception();
			}
		}
	}
	private void writeSimpleModeChoice(Population population, String outFile) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(outFile);
		for(Person person:population.getPersons().values())
			for(int p1=0; p1<person.getPlans().size(); p1++)
				for(int p2=p1+1; p2<person.getPlans().size(); p2++)
					if(sameChainDifferentMode(person.getPlans().get(p1), person.getPlans().get(p2)))
						writePlans(writer, person.getId(), person.getPlans().get(p1), person.getPlans().get(p2));
		writer.close();
	}
	private void writePlans(PrintWriter writer, Id<Person> personId, Plan plan1, Plan plan2) {
		String plan = personId+"("+plan1.getScore()+"): ";
		for(PlanElement planElement:plan1.getPlanElements())
			plan+=(planElement instanceof Activity?((Activity)planElement).getType(): ((Leg)planElement).getMode()+(((Leg)planElement).getMode().equals("pt")?"("+(((Leg)planElement).getRoute()).getRouteDescription()+")":""))+"   ";
		writer.println(plan);
		plan = personId+"("+plan2.getScore()+"): ";
		for(PlanElement planElement:plan2.getPlanElements())
			plan+=(planElement instanceof Activity?((Activity)planElement).getType(): ((Leg)planElement).getMode()+(((Leg)planElement).getMode().equals("pt")?"("+(((Leg)planElement).getRoute()).getRouteDescription()+")":""))+"   ";
		writer.println(plan);
	}
	private boolean sameChainDifferentMode(Plan plan1, Plan plan2) {
		List<Tuple<String, Coord>> activities1 = new ArrayList<Tuple<String, Coord>>();
		List<String> legModes1 = new ArrayList<String>();
		boolean inActivity=true;
		for(PlanElement planElement:plan1.getPlanElements())
			if(planElement instanceof Activity && !((Activity)planElement).getType().equals("pt interaction")) {
				activities1.add(new Tuple<String, Coord>(((Activity)planElement).getType(), ((Activity)planElement).getCoord()));
				inActivity = true;
			}
			else if(planElement instanceof Leg && inActivity) {
				inActivity = false;
				if(((Leg)planElement).getMode().equals("transit_walk"))
					legModes1.add("pt");
				else
					legModes1.add(((Leg)planElement).getMode());
			}
		List<Tuple<String, Coord>> activities2 = new ArrayList<Tuple<String, Coord>>();
		List<String> legModes2 = new ArrayList<String>();
		inActivity=true;
		for(PlanElement planElement:plan2.getPlanElements())
			if(planElement instanceof Activity && !((Activity)planElement).getType().equals("pt interaction")) {
				activities2.add(new Tuple<String, Coord>(((Activity)planElement).getType(), ((Activity)planElement).getCoord()));
				inActivity = true;
			}
			else if(planElement instanceof Leg && inActivity) {
				inActivity = false;
				if(((Leg)planElement).getMode().equals("transit_walk"))
					legModes2.add("pt");
				else
					legModes2.add(((Leg)planElement).getMode());
			}
		boolean sameChain = true;
		if(activities1.size() != activities2.size())
			sameChain = false;
		else
			for(int a=0; a<activities1.size(); a++)
				if(!(activities1.get(a).getFirst().equals(activities2.get(a).getFirst()) && activities1.get(a).getSecond().equals(activities2.get(a).getSecond())))
					sameChain = false;
		boolean differentMode = false;
		if(legModes1.size() != legModes2.size())
			differentMode = true;
		else
			for(int l=0; l<legModes1.size(); l++)
				if(!legModes1.get(l).equals(legModes2.get(l)))
					differentMode = true;
		return sameChain && differentMode;
	}
	

	//Main
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		new ComparePlans(args[0], args[1], args[2]);
	}
	
}
