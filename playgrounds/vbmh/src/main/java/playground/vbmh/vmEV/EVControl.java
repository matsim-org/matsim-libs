package playground.vbmh.vmEV;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import playground.vbmh.controler.VMConfig;
import playground.vbmh.vmParking.ParkHistoryWriter;
import playground.vbmh.vmParking.VMScoreKeeper;

import javax.xml.bind.JAXB;
import java.io.File;
import java.util.Map;
/**
 * 
 * Manages charging and energy Consumption of EVs etc.
 * Provides EV related informations to other classes.
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */



public class EVControl {
	private Controler controler;
	private EVList evList;
	ParkHistoryWriter phwriter = new ParkHistoryWriter();
	
	public void startUp(String evFilename, Controler controler){
		this.controler=controler;
		
		//EVs Laden
		File evFile = new File( evFilename );
		EVList evList = JAXB.unmarshal( evFile, EVList.class ); //Laedt EVs XML
		this.evList=evList;
	}
	
	//------------------------------------------------------------------------------------
	
	public void linkLeave(LinkLeaveEvent event){ //!! Sicherstellen dass nicht von fussgaenger ausgeloest wird
		Id personID = event.getDriverId();
		if (this.evList.hasEV(personID)){
			//System.out.println("EV am umme cruise");
		} else{
			return;
		}

        Link link = controler.getScenario().getNetwork().getLinks().get(event.getLinkId());
		double time = event.getTime();
        Map<String, Object> personAttributes = controler.getScenario().getPopulation().getPersons().get(personID).getCustomAttributes();
		VMScoreKeeper scorekeeper;
		if (personAttributes.get("VMScoreKeeper")!= null){
			scorekeeper = (VMScoreKeeper) personAttributes.get("VMScoreKeeper");
		} else{
			scorekeeper = new VMScoreKeeper();
			personAttributes.put("VMScoreKeeper", scorekeeper);
		}
		
		EV ev = evList.getEV(personID);
		double consumption = ev.calcEnergyConsumption(link, time);
		evList.getEV(personID).discharge(consumption);
		
		double distance = link.getLength();
		double safedFuelCostUtil = VMConfig.evSavingsPerKM * distance;
		scorekeeper.add(safedFuelCostUtil);
		
		if(ev.stateOfCharge<0){
			phwriter.addEVOutOfBattery(Double.toString(time), personID.toString());
			
			scorekeeper.add(-30);
		}
		
		
		//System.out.println(consumption);
		//System.out.println(ev.getStateOfChargePercentage());
		
		
		
		
	} 
	
//------------------------------------------------------------------------------------
	
	public void linkEnter(LinkEnterEvent event){
		Id personID = event.getDriverId();
		if (!this.evList.hasEV(personID)){
			return;
		}
		
		evList.getEV(personID).setEnteredLinkTime(event.getTime());
		
		
	}
	
	public void iterStart(){
		this.evList.setAllStateOfChargePercentage(80.0); //!! aus der config nehmen
		
	}
	
	public boolean hasEV(Id personId){
		return evList.hasEV(personId);
	}
	
	
	public double charge(Id personId, double chargingRate, double duration){
		EV ev = evList.getEV(personId);
		//double amountOfEnergy = ev.calcNewStateOfCharge(chargingRate, duration)-ev.stateOfCharge;
		double amountOfEnergy=clalcChargedAmountOfEnergy(personId, chargingRate, duration);
		//System.out.println("amount of energy :" + amountOfEnergy);
		ev.setStateOfCharge(ev.calcNewStateOfCharge(chargingRate, duration));
		return amountOfEnergy;
	}
	
	public double stateOfChargePercentage(Id personId){
		return evList.getEV(personId).getStateOfChargePercentage();
	}
	
	
	public double calcNewStateOfChargePercentage(Id personId, double chargingRate, double duration){
		EV ev = evList.getEV(personId);
		return 100*ev.calcNewStateOfCharge(chargingRate, duration)/ev.batteryCapacity;
	}
	
	
	public double clalcChargedAmountOfEnergy(Id personId, double chargingRate, double duration){
		//Returns the amount of Energy which can be charged for the given EV at the given charging Rate
		//during the given time in kwh
		EV ev = evList.getEV(personId);
		
		if(duration<0){
			System.out.println("E R R O R: Negative duration observed >> Set chargeable amount of engery to zero (Not fatal)");
			return 0.0;
		}
		
		return ev.calcNewStateOfCharge(chargingRate, duration)-ev.stateOfCharge;
		
	}
	
	
	
	public double calcEnergyConsumptionForDistancePerc(Id personId, double distance){
		return evList.getEV(personId).calcEnergyConsumptionForDistancePerc(distance);
	}
	
	
	/*
	public double getRestOfDayEnergyConsumption(Id personId){
		Person person = controler.getPopulation().getPersons().get(personId);
		
		return 1;
	}
	
	*/
	
	
	

}
