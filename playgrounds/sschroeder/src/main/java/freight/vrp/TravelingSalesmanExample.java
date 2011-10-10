package freight.vrp;

import java.util.Collection;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mzilske.freight.carrier.Tour;
import playground.mzilske.freight.carrier.Tour.TourElement;
import vrp.basics.ManhattanCosts;

public class TravelingSalesmanExample {
	
	public static void main(String[] args) {
		/*
		 * start@i(1,0)
		 * sightseeing@i(5,0)
		 * refreshing@j(8,1)
		 * geochaching@i(4,8)
		 */
		Logger.getRootLogger().setLevel(Level.INFO);
		Config config = ConfigUtils.createConfig();
		Scenario scen = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scen).readFile("networks/grid.xml");
		
		TravelingSalesmanProblemBuilder travelingSalesmanProblemBuilder = new TravelingSalesmanProblemBuilder(scen.getNetwork());
		travelingSalesmanProblemBuilder.addActivity("sightseeing", makeId("i(5,0)"), 14*3600, 16*3600, 3600);
		travelingSalesmanProblemBuilder.addActivity("refreshing", makeId("j(8,1)"), 12*3600, 14*3600, 3600);
		travelingSalesmanProblemBuilder.addActivity("geocaching", makeId("i(4,8)"), 8*3600, 16*3600, 3*3600);
		travelingSalesmanProblemBuilder.addActivity("drinkingBeer", makeId("j(8,4)"), 13*3600, 22*3600, 1800);
		travelingSalesmanProblemBuilder.setStart(makeId("i(1,0)"), 7*3600, 18*3600);
		ManhattanCosts manhattanCosts = new ManhattanCosts(); 
		manhattanCosts.speed = 3.0;
		travelingSalesmanProblemBuilder.setCosts(manhattanCosts);
		
		TravelingSalesman travelingSalesman = new TravelingSalesman(travelingSalesmanProblemBuilder.buildTSP());
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename("output/salesman.png");
		travelingSalesman.registerListener(chartListener);
		travelingSalesman.registerListener(new RuinAndRecreateReport());
		travelingSalesman.run();
		
		print(travelingSalesman.getSolution());
	}

	private static void print(Collection<Tour> solution) {
		for(Tour t : solution){
			boolean firstElement = true;
			for(TourElement e : t.getTourElements()){
				if(firstElement){
					System.out.println("activity="+e.getActivityType()+";earliestDeparture=" + getTime(e.getTimeWindow().getStart()) + ";latestDeparture=" + getTime(e.getTimeWindow().getEnd()));
					firstElement = false;
				}
				else{
					System.out.println("activity="+e.getActivityType()+";earliestArrival=" + getTime(e.getTimeWindow().getStart()) + ";latestArrival=" + getTime(e.getTimeWindow().getEnd()));
				}
				
			}
		}
		
	}

	private static String getTime(double start) {
		long hour = (long)Math.floor(start/3600);
		long minute = Math.round((start%3600)/3600*60);
		String hourS = null;
		String minuteS = null;
		if(hour<10){
			hourS = "0"+hour; 
		}
		else{
			hourS = "" + hour;
		}
		if(minute<10){
			minuteS = "0" + minute;
		}
		else{
			minuteS = "" + minute;
		}
		return hourS+":"+minuteS;
	}

	private static Id makeId(String string) {
		return new IdImpl(string);
	}

}
