package playground.balac.avignon;

import java.io.IOException;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class ModalSplitGrocery {
	
	private double carShare = 0.0;
	private double ptShare = 0.0;
	private double bikeShare = 0.0;
	private double walkShare = 0.0;
	private double walkDistance = 0.0;
	private double carDistance = 0.0;
	private double bikeDistance = 0.0;
	private double ptDistance = 0.0;
	private double walkTime = 0.0;
	private double carTime = 0.0;
	private double bikeTime = 0.0;
	private double ptTime = 0.0;
	private int countLicenceHolders = 0;
	private int countNoLicenceHolders = 0;
	private int trips = 0;

	public void run(String input, String attributes) throws IOException {
		double centerX = 683217.0; 
		double centerY = 247300.0;
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(attributes);	
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

			PopulationReader populationReader = new MatsimPopulationReader(scenario);
			populationReader.readFile(input);
	
			int count = 0;
			int countCar = 0;
			int countBike = 0;
			int countWalk = 0;
			int countPt = 0;
			
			int counttrips = 0;

			Population pop = scenario.getPopulation();	
			for (Person p:pop.getPersons().values()) {
				if (bla.getAttribute(p.getId().toString(), "subpopulation") == null ) {
					if (((PersonImpl)p).getLicense().equals("yes"))
						countLicenceHolders++;
					else
						countNoLicenceHolders++;

				Leg previousLeg = null;
				boolean act = true;
				boolean previousactivity = false;
				boolean lastactivity = false;
				boolean ptint = false;
				Activity firsta = null;
				Activity seconda = null;	
				double tempTTPT = 0.0;

				int co = 0;
				for (PlanElement pe:p.getSelectedPlan().getPlanElements()) {
					
					if (pe instanceof Leg) {
						previousLeg = (Leg) pe;
						if (previousLeg.getMode().equals("pt") || previousLeg.getMode().equals("transit_walk"))
							tempTTPT += previousLeg.getTravelTime();
						
					}
					else if (pe instanceof Activity ) {
							if (!((Activity) pe).getType().equals("pt interaction")) {
								act = true;
								firsta = seconda;
								seconda = (Activity) pe;
								co++;
							}
							else
								ptint = true;
							
						
							previousactivity = lastactivity;
							if (!((Activity) pe).getType().equals("pt interaction")){
								if (Math.sqrt(Math.pow(((Activity) pe).getCoord().getX() - centerX, 2) +(Math.pow(((Activity) pe).getCoord().getY() - centerY, 2))) < 30000) 
									lastactivity = true;
								else
									lastactivity = false;
								
								if (previousactivity && lastactivity) {
									if (previousLeg !=null) {
								if (previousLeg.getMode().equals( "car" )) {
									countCar++;
									carDistance += Math.sqrt(Math.pow((firsta).getCoord().getX() - (seconda).getCoord().getX(), 2) +(Math.pow((firsta).getCoord().getY() - (seconda).getCoord().getY(), 2)));
									carTime += previousLeg.getTravelTime();
									count++;
								}
								else if (previousLeg.getMode().equals("bike")) {
									countBike++;
									bikeDistance += Math.sqrt(Math.pow((firsta).getCoord().getX() - (seconda).getCoord().getX(), 2) +(Math.pow((firsta).getCoord().getY() - (seconda).getCoord().getY(), 2)));
									bikeTime += previousLeg.getTravelTime();

									count++;
								}
								else if (ptint && act) {
									ptint = false;
									act = false;
									ptDistance += Math.sqrt(Math.pow((firsta).getCoord().getX() - (seconda).getCoord().getX(), 2) +(Math.pow((firsta).getCoord().getY() - (seconda).getCoord().getY(), 2)));
									ptTime += tempTTPT;
									tempTTPT = 0.0;
									countPt++;
									count++;
								}
								else  {
									countWalk++;
									if (previousLeg.getMode().equals("transit_walk"))
										tempTTPT = 0.0;
									walkDistance += Math.sqrt(Math.pow((firsta).getCoord().getX() - (seconda).getCoord().getX(), 2) +(Math.pow((firsta).getCoord().getY() - (seconda).getCoord().getY(), 2)));
									walkTime += previousLeg.getTravelTime();

									count++;
									}
	
									}
								}
								else {
									ptint = false;
									act = false;
									tempTTPT = 0.0;
								}
					}
							}
					
				}
				counttrips += (co-1);
				
			}
			}
			trips = count;
			carShare = 100.0 * (double)countCar/(double)count;
			bikeShare = 100.0 * (double)countBike/(double)count;
			walkShare = 100.0 * (double)countWalk/(double)count;
			ptShare = 100.0 * (double)countPt/(double)count;	
			
			carDistance /= (double)countCar;
			walkDistance /= (double)countWalk;
			bikeDistance /= (double)countBike;
			ptDistance /= (double)countPt;
			
			carTime /= (double)countCar;
			walkTime /= (double)countWalk;
			bikeTime /= (double)countBike;
			ptTime /= (double)countPt;
			System.out.println(countNoLicenceHolders);
			System.out.println(countLicenceHolders);

		
	}
	
	
	
	
	public double getCarShare() {
		return carShare;
	}

	public double getPtShare() {
		return ptShare;
	}

	public double getBikeShare() {
		return bikeShare;
	}

	public double getWalkShare() {
		return walkShare;
	}
	
	public double getWalkDistance() {
		return walkDistance;
	}


	public double getCarDistance() {
		return carDistance;
	}


	public double getBikeDistance() {
		return bikeDistance;
	}


	public double getPtDistance() {
		return ptDistance;
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		ModalSplitGrocery b = new ModalSplitGrocery();
		b.run(args[0], args[1]);
		System.out.println(b.getCarShare());
		System.out.println(b.getBikeShare());
		System.out.println(b.getWalkShare());
		System.out.println(b.getPtShare());
		System.out.println(b.getCarDistance());
		System.out.println(b.getBikeDistance());
		System.out.println(b.getWalkDistance());
		System.out.println(b.getPtDistance());
		System.out.println(b.getCarTime());
		System.out.println(b.getBikeTime());
		System.out.println(b.getWalkTime());
		System.out.println(b.getPtTime());
		System.out.println(b.getTrips());

	
	}


	public int getTrips() {
		return trips;
	}


	public double getWalkTime() {
		return walkTime;
	}


	public double getCarTime() {
		return carTime;
	}


	public double getBikeTime() {
		return bikeTime;
	}


	public double getPtTime() {
		return ptTime;
	}

	
}
