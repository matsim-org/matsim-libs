/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.vsp.energy.eVehicles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.energy.energy.ChargingProfiles;
import playground.vsp.energy.energy.DisChargingProfiles;
import playground.vsp.energy.poi.PoiList;
import playground.vsp.energy.validation.ValidationInfoWriter;
import playground.vsp.energy.validation.ValidationInformation;

/**
 * @author droeder
 *
 */
public class EVehicles {
	private static final Logger log = Logger.getLogger(EVehicles.class);

	private ChargingProfiles cp;
	private DisChargingProfiles dp;

	private Network net;
	private HashMap<Id, EVehicle> vehicles;

	private PoiList poilist;

	public EVehicles(ChargingProfiles cp, DisChargingProfiles dp, Network net, PoiList poi){
		this.cp = cp;
		this.dp = dp;
		this.net = net;
		this.vehicles = new HashMap<Id, EVehicle>();
		this.poilist = poi;
	}
	
	public void addVehicle(EVehicle v){
		this.vehicles.put(v.getId(), v);
	}
	
	public void handleEvent(Event e){
		Link l;
		EVehicle v;
		double value;
		if(e instanceof LinkEnterEvent){
			if(this.vehicles.containsKey(((LinkEnterEvent) e).getVehicleId())){
				l = this.net.getLinks().get(((LinkEnterEvent) e).getLinkId());
				v = this.vehicles.get(((LinkEnterEvent) e).getVehicleId());
				// used to calculate the average speed, when discharging
				v.setLinkEnter(l.getLength(), e.getTime());
			}
		}else if(e instanceof LinkLeaveEvent){
			if(this.vehicles.containsKey(((LinkLeaveEvent) e).getVehicleId())){
				v = this.vehicles.get(((LinkLeaveEvent) e).getVehicleId());
				value = this.dp.getJoulePerKm(v.getProfileId(), v.getLastLinkLength()/(e.getTime() - v.getLinkEnterTime()), 0.);
				v.discharge(e.getTime(), value);
			}
		}else if(e instanceof PersonEntersVehicleEvent){
			if(this.vehicles.containsKey(((PersonEntersVehicleEvent) e).getVehicleId())){
				v = this.vehicles.get(((PersonEntersVehicleEvent) e).getVehicleId());
				//unplug vehicle. store info in poilist
				if(v.unplug(this.poilist, e.getTime())){
					//calculate next state if charging was possible
					value = this.cp.getNewState(v.getProfileId(), (v.getChargingEnd(e.getTime()) - v.getChargingStart()) / 60, v.getSoC());
					v.charge(v.getChargingEnd(e.getTime()), value);
				}else{
					//otherwise store current value (just for creating a figure)
					v.charge(v.getChargingEnd(e.getTime()), v.getSoC());
				}
				//next planelement
				v.increase();
				//person enters the link at the end and start it's ride
				v.setLinkEnter(0., e.getTime());
				//should log an error, if another then the expected person enters the vehicle //TODO untested
				this.rightPerson((PersonEntersVehicleEvent)e);
			}
		}else if(e instanceof PersonLeavesVehicleEvent){
			if(this.vehicles.containsKey(((PersonLeavesVehicleEvent) e).getVehicleId())){
				v = this.vehicles.get(((PersonLeavesVehicleEvent) e).getVehicleId());
				value = this.dp.getJoulePerKm(v.getProfileId(), v.getLastLinkLength()/(e.getTime() - v.getLinkEnterTime()), 0.);
				v.discharge(e.getTime(), value);
				v.increase();
				//not the real linkEnterTime but used for calculation. maybe better last activityTime...
				v.setLinkEnter(0., e.getTime());
				// plug vehicle. store if charging is possible. store POI-Info
				v.plug(this.poilist, e.getTime());
			}			
		}else{
			log.error("this should never happen...");
		}
	}
	
	private void rightPerson(PersonEntersVehicleEvent e){
		if(!this.vehicles.containsKey(e.getVehicleId())){
			//do nothing
		}else if(!this.vehicles.get(e.getVehicleId()).rightPerson(e.getPersonId())){
			log.error("Person " + e.getPersonId() + " drives with vehicle " + e.getVehicleId() +
				"but is not registered for this leg...");
		}
	}

	/**
	 * @param info 
	 * @param oUTDIR
	 */
	public void dumpData(String outdir, ValidationInformation info) {
		BufferedWriter w;
		//dump vehicle-data
		File f = new File(outdir);
		if(!f.exists()){
			f.mkdirs();
			log.info("created " + outdir + "...");
		}
		log.info("start writing Vehicle-Energy-Data...");
		for(EVehicle v: this.vehicles.values()){
			w = IOUtils.getBufferedWriter(outdir + v.getId().toString().replace("_", "") + ".dat");
			try {
				w.write("#SoC;time;dist;\n");
				Double[] time, dist, soc;
				time = v.getSoCChangeTimes();
				dist = v.getTravelledDistances();
				soc = v.getSoCs();
				for(int i = 0; i < time.length; i++){
					w.write(String.valueOf(soc[i]) + "\t");	
					w.write(String.valueOf(time[i]) + "\t");
					w.write(String.valueOf(dist[i]) + "\t\n");
				}
				w.flush();
				w.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.info("finished...");
		
		this.poilist.editTimeInfo(info);
		new ValidationInfoWriter(info).writeFile(outdir + "poiInfo.xml");
	}
}
