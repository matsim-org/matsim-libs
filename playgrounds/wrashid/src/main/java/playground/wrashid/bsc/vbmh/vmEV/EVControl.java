package playground.wrashid.bsc.vbmh.vmEV;

import java.io.File;

import javax.xml.bind.JAXB;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.wrashid.bsc.vbmh.vmParking.ParkingMap;

public class EVControl {
	private Controler controler;
	private EVList evList;
	
	public void startUp(String evFilename, Controler controler){
		this.controler=controler;
		
		//EVs Laden
		File evFile = new File( evFilename );
		EVList evList = JAXB.unmarshal( evFile, EVList.class ); //Laedt EVs XML
		this.evList=evList;
	}
	
	//------------------------------------------------------------------------------------
	
	public void linkLeave(LinkLeaveEvent event){
		Id personID = event.getPersonId();
		if (this.evList.hasEV(personID)){
			//System.out.println("EV am umme cruise");
		} else{
			return;
		}
		
		Link link = controler.getNetwork().getLinks().get(event.getLinkId());
		double time = event.getTime();
		
		EV ev = evList.getEV(personID);
		double consumption = ev.calcEnergyConsumption(link, time);
		evList.getEV(personID).discharge(consumption);
		System.out.println(consumption);
		System.out.println(ev.getStateOfChargePercentage());
		
		
		
		
	} 
	
//------------------------------------------------------------------------------------
	
	public void linkEnter(LinkEnterEvent event){
		Id personID = event.getPersonId();
		if (!this.evList.hasEV(personID)){
			return;
		}
		
		evList.getEV(personID).setEnteredLinkTime(event.getTime());
		
		
	}
	
	public void iterStart(){
		this.evList.setAllStateOfChargePercentage(50.0);
		
	}
	
	
	
	

}
