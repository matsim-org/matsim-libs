package playground.wrashid.bsc.vbmh.vmParking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.wrashid.bsc.vbmh.util.CSVWriter;
import playground.wrashid.bsc.vbmh.util.RemoveDuplicate;
import playground.wrashid.bsc.vbmh.util.VMCharts;
import playground.wrashid.bsc.vbmh.vmEV.EVControl;

public class IterEndStats {
	public void run(ParkControl parkControl){
		Controler controler = parkControl.controller;
		if(!parkControl.evUsage){
			System.out.println("Iteration end stats does not work without EV usage");
			return;
		}
		
		LinkedList<Id> totEVDriving=new LinkedList<Id>();
		LinkedList<Id> totNEVDriving=new LinkedList<Id>();
		LinkedList<Id> totEVNotDriving=new LinkedList<Id>();
		LinkedList<Id>	totNEVNotDriving=new LinkedList<Id>();
		LinkedList<Id> secEVDriving=new LinkedList<Id>();
		LinkedList<Id> secNEVDriving=new LinkedList<Id>();
		LinkedList<Id> secEVNotDriving=new LinkedList<Id>();
		LinkedList<Id>	secNEVNotDriving=new LinkedList<Id>();
		LinkedList<Id> workEVDriving=new LinkedList<Id>();
		LinkedList<Id> workNEVDriving=new LinkedList<Id>();
		LinkedList<Id> workEVNotDriving=new LinkedList<Id>();
		LinkedList<Id>	workNEVNotDriving=new LinkedList<Id>();
		LinkedList<Id> totCharged=new LinkedList<Id>();
		LinkedList<Id> workCharged=new LinkedList<Id>();
		LinkedList<Id> secCharged=new LinkedList<Id>();
		LinkedList<Id> totHadToCharge=new LinkedList<Id>();
		LinkedList<Id> workHadToCharge=new LinkedList<Id>();
		LinkedList<Id> secHadToCharge=new LinkedList<Id>();
		LinkedList<Id> totOutOfBattery=new LinkedList<Id>();
		LinkedList<Id> workOutOfBattery=new LinkedList<Id>();
		LinkedList<Id> secOutOfBattery=new LinkedList<Id>();
		LinkedList<Id> totEvNotParked=new LinkedList<Id>();
		LinkedList<Id> workEvNotParked=new LinkedList<Id>();
		LinkedList<Id> secEvNotParked=new LinkedList<Id>();
		LinkedList<Id> totNEvNotParked=new LinkedList<Id>();
		LinkedList<Id> workNEvNotParked=new LinkedList<Id>();
		LinkedList<Id> secNEvNotParked=new LinkedList<Id>();
		
		
		VMCharts chart = new VMCharts();
		chart.addChart("Util vs Traveldistance");
		chart.setAx("Util vs Traveldistance", false);
		chart.setBox("Util vs Traveldistance", true);
		chart.setInterval("Util vs Traveldistance", 10000);
		chart.addSeries("Util vs Traveldistance", "NEV");
		chart.addSeries("Util vs Traveldistance", "EV");	
		
		chart.addChart("Util vs Traveldistance EV");
		chart.setAx("Util vs Traveldistance EV", false);
		chart.setBox("Util vs Traveldistance EV", true);
		chart.setInterval("Util vs Traveldistance EV", 10000);
		chart.addSeries("Util vs Traveldistance EV", "EV");
		
		
		EVControl evControl = parkControl.evControl;
		
		
		for (Person person : controler.getPopulation().getPersons().values()){
			PersonImpl personImpl = (PersonImpl) person;
			double soc;
			boolean notParked=false;
			PlanImpl planImpl=(PlanImpl)personImpl.getSelectedPlan();
			LegImpl legImpl=(LegImpl)planImpl.getNextLeg(planImpl.getFirstActivity());
			Id id = person.getId();
			Activity firstAct = planImpl.getFirstActivity();
			String actType = planImpl.getNextActivity(legImpl).getType();
			ParkingSpot selectedSpot=(ParkingSpot)person.getCustomAttributes().get("selectedParkingspot");
			if(evControl.hasEV(person.getId())&&selectedSpot!=null){
				soc= evControl.stateOfChargePercentage(person.getId());
			}else soc=-150;
			if(selectedSpot==null){
				selectedSpot = new ParkingSpot();
				notParked=true;
			}
			
			
			double score = personImpl.getSelectedPlan().getScore();
			double distance = getPlanDistance(planImpl);
			if (score<0){
				score = -1;
			}
			if(evControl.hasEV(person.getId())){
				VMCharts.addValues("Util vs Traveldistance", "EV", distance, score);
				VMCharts.addValues("Util vs Traveldistance EV", "EV", distance, score);
			}else{
				VMCharts.addValues("Util vs Traveldistance", "NEV", distance, score);
				}
			
			
			if(evControl.hasEV(person.getId())){
				if(legImpl.getMode().equals("car")){
					totEVDriving.add(id);
					if(selectedSpot.charge){
						totCharged.add(id);
					}
					if(soc<50){
						totHadToCharge.add(id);
					}
					if(soc<0){
						totOutOfBattery.add(id);
					}
					if(notParked){
						totEvNotParked.add(id);
					}
					if(actType.equals("work")){
						workEVDriving.add(id);
						if(selectedSpot.charge){
							workCharged.add(id);
						}
						if(soc<50){
							workHadToCharge.add(id);
						}
						if(soc<0){
							workOutOfBattery.add(id);
						}
						if(notParked){
							workEvNotParked.add(id);
						}
					}else if(actType.equals("secondary")){
						secEVDriving.add(id);
						if(selectedSpot.charge){
							secCharged.add(id);
						}
						if(soc<50){
							secHadToCharge.add(id);
						}
						if(soc<0){
							secOutOfBattery.add(id);
						}
						if(notParked){
							secEvNotParked.add(id);
						}
					}
				}else{
					totEVNotDriving.add(id);
					if(actType.equals("work")){
						workEVNotDriving.add(id);
					}else if(actType.equals("secondary")){
						secEVNotDriving.add(id);
					}
				}
			}else{
				if(legImpl.getMode().equals("car")){
					totNEVDriving.add(id);
					if(notParked){
						totNEvNotParked.add(id);
					}
					if(actType.equals("work")){
						workNEVDriving.add(id);
						if(notParked){
							workNEvNotParked.add(id);
						}
					}else if(actType.equals("secondary")){
						secNEVDriving.add(id);
						if(notParked){
							secNEvNotParked.add(id);
						}
					}
				}else{
					totNEVNotDriving.add(id);
					if(actType.equals("work")){
						workNEVNotDriving.add(id);
					}else if(actType.equals("secondary")){
						secNEVNotDriving.add(id);
					}
				}
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
		
		CSVWriter writer = new CSVWriter(controler.getConfig().getModule("controler").getValue("outputDirectory")+"/parkhistory/parkstats_"+controler.getIterationNumber());
		writer.writeLine("Total: ");
		writer.writeLine("EVs in use: "+totEVDriving.size());
		writer.writeLine("EVs not in use: "+totEVNotDriving.size());
		writer.writeLine("charged: "+totCharged.size());
		writer.writeLine("had to charge: "+totHadToCharge.size());
		writer.writeLine("out of battery: "+totOutOfBattery.size());
		writer.writeLine("EVs not parked: "+totEvNotParked.size());
		writer.writeLine("NEVs in use: "+totNEVDriving.size());
		writer.writeLine("NEVs not in use: "+totNEVNotDriving.size());
		writer.writeLine("NEVs not parked: "+totNEvNotParked.size());
		writer.writeLine("");
		writer.writeLine("Work: ");
		writer.writeLine("EVs in use: "+workEVDriving.size());
		writer.writeLine("EVs not in use: "+workEVNotDriving.size());
		writer.writeLine("charged: "+workCharged.size());
		writer.writeLine("had to charge: "+workHadToCharge.size());
		writer.writeLine("out of battery: "+workOutOfBattery.size());
		writer.writeLine("EVs not parked: "+workEvNotParked.size());
		writer.writeLine("NEVs in use: "+workNEVDriving.size());
		writer.writeLine("NEVs not in use: "+workNEVNotDriving.size());
		writer.writeLine("NEVs not parked: "+workNEvNotParked.size());
		writer.writeLine("");
		writer.writeLine("Secondary: ");
		writer.writeLine("EVs in use: "+secEVDriving.size());
		writer.writeLine("EVs not in use: "+secEVNotDriving.size());
		writer.writeLine("charged: "+secCharged.size());
		writer.writeLine("had to charge: "+secHadToCharge.size());
		writer.writeLine("out of battery: "+secOutOfBattery.size());
		writer.writeLine("EVs not parked: "+secEvNotParked.size());
		writer.writeLine("NEVs in use: "+secNEVDriving.size());
		writer.writeLine("NEVs not in use: "+secNEVNotDriving.size());
		writer.writeLine("NEVs not parked: "+secNEvNotParked.size());
		writer.writeLine("");
		
		writer.close();
		
		
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
