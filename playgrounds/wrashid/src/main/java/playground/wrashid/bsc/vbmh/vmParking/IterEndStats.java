package playground.wrashid.bsc.vbmh.vmParking;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.wrashid.bsc.vbmh.util.VMCharts;
import playground.wrashid.bsc.vbmh.vmEV.EVControl;

public class IterEndStats {
	public void run(ParkControl parkControl){
		Controler controler = parkControl.controller;
		if(!parkControl.evUsage){
			System.out.println("Iteration end stats do not work without EV usage");
			return;
		}
		
		
		VMCharts chart = new VMCharts();
		chart.addChart("Util vs Traveldistance");
		chart.setAx("Util vs Traveldistance", false);
		chart.addSeries("Util vs Traveldistance", "NEV");
		chart.addSeries("Util vs Traveldistance", "EV");	
		
		chart.addChart("Util vs Traveldistance EV");
		chart.setAx("Util vs Traveldistance EV", false);
		chart.addSeries("Util vs Traveldistance EV", "EV");
		
		
		EVControl evControl = parkControl.evControl;
		for (Person person : controler.getPopulation().getPersons().values()){
			PersonImpl personImpl = (PersonImpl) person;
			double score = personImpl.getSelectedPlan().getScore();
			double distance = getPlanDistance((PlanImpl) personImpl.getSelectedPlan());
			if (score<0){
				score = -1;
			}
			if(evControl.hasEV(person.getId())){
				VMCharts.addValues("Util vs Traveldistance", "EV", distance, score);
				VMCharts.addValues("Util vs Traveldistance EV", "EV", distance, score);
			}else{
				VMCharts.addValues("Util vs Traveldistance", "NEV", distance, score);
				}
		}
	
		chart.addChart("Parkinglot occupancy");
		chart.setAx("Parkinglot occupancy", false);
		chart.setAxis("Parkinglot occupancy", "time", "occupancy [0..1]");
		chart.setLine("Parkinglot occupancy", true);
		for(Parking parking : parkControl.parkingMap.getParkings()){
			if(parking.ocupancyStats == true){
				chart.addSeries("Parkinglot occupancy", Integer.toString(parking.id)+"EV");
				chart.addSeries("Parkinglot occupancy", Integer.toString(parking.id)+"NEV");
				for(Double[] values : parking.occupancyList){
					VMCharts.addValues("Parkinglot occupancy", Integer.toString(parking.id)+"EV", values[0], values[2]);
					VMCharts.addValues("Parkinglot occupancy", Integer.toString(parking.id)+"NEV", values[0], values[2]);
				}
				
			}
		}
		
		
		
		
		
		
	}
	
	
	double getPlanDistance(PlanImpl plan){
		double distance=0;
		for (PlanElement element : plan.getPlanElements()){
			if(element.getClass()==LegImpl.class){
				Leg leg = (Leg)element;
				distance+=leg.getRoute().getDistance();
			}
		}
		return distance;
		
	}
	
}
