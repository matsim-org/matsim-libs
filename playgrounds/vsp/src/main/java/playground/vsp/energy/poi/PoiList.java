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
package playground.vsp.energy.poi;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;

import playground.vsp.energy.validation.PoiInfo;
import playground.vsp.energy.validation.PoiTimeInfo;
import playground.vsp.energy.validation.ValidationInformation;

/**
 * @author droeder
 *
 */
public class PoiList {

	private int slotSize;
	private HashMap<Id<Poi>, List<Slot>> slots;

	public PoiList(ValidationInformation info, int slotSize){
		this.slots = new HashMap<Id<Poi>, List<Slot>>();
		this.initSlotMap(0, info);
		this.slotSize = slotSize;
	}
	
	public boolean plug(Id<Poi> id, double time){
		if(!this.slots.containsKey(id)){
			return false;
		}
		int slot = this.getSlot(time);
		if(this.slots.get(id).size() <= slot){
			initSlot(id, slot);
		}
		return this.slots.get(id).get(slot).plug();
	}
	
	public void unplug(Id<Poi> id, double time){
//		if(!this.slots.containsKey(id)){
//			return false;
//		}
		int slot = this.getSlot(time);
		if(this.slots.get(id).size() <= slot){
			initSlot(id, slot);
		}
		this.slots.get(id).get(slot).unplug();
	}
	
	/**
	 * @param id
	 * @param slot
	 */
	private void initSlot(Id<Poi> id, int slot) {
		List<Slot> slots = this.slots.get(id);
		Slot s = slots.get(slots.size() -1);
		for(int i = (s.getSlotIndex()+1); i < (slot + 1); i++){
			slots.add(new Slot(s.getCap(), i, s.getOccupation()));
		}
		this.slots.put(id, slots);
	}
	
	/**
	 * @param id 
	 * @param slot
	 */
	private void initSlotMap(int slot, ValidationInformation info) {
		for(PoiInfo i: info.getValidationInformationList()){
			List<Slot> list = new ArrayList<Slot>();
			list.add(new Slot(i.getMaximumCapacity(), slot, 0.));
			this.slots.put(Id.create(i.getPoiID(), Poi.class), list);
		}
	}

	public int getSlot(Double time){
		return (int) (time/this.slotSize);
	}
	
	public void editTimeInfo(ValidationInformation info){
		List<Slot> slots;
		PoiTimeInfo ti;
		int start, end;
		for(PoiInfo i: info.getValidationInformationList()){
			if(this.slots.containsKey(Id.create(i.getPoiID(), Poi.class))){
				slots = this.slots.get(Id.create(i.getPoiID(), Poi.class));
				for(Slot s: slots){
					ti = new PoiTimeInfo();
					start = this.slotSize * s.getSlotIndex();
					end = start + this.slotSize;
					ti.setStartTime(new GregorianCalendar(1979, 01, 01, 0, 0, start));
					ti.setEndTime(new GregorianCalendar(1979, 01, 01, 0, 0, end));
					ti.setUsedCapacity(s.getMaxOccupation());
					i.getPoiTimeInfos().add(ti);
				}
			}else{
				continue;
			}
		}
	}
	
	private class Slot{
		private Double cap;
		private int slotIndex;
		private Double occupation;
		private Double maxOccupation;

		public Slot(Double cap, int slotIndex, Double initialOccupation){
			this.cap = cap;
			this.slotIndex = slotIndex;
			this.occupation = initialOccupation;
			this.maxOccupation = initialOccupation;
		}
		
		public boolean plug(){
			if(this.occupation >= this.cap){
				return false;
			}else{
				this.occupation++;
				if(this.occupation > this.maxOccupation){
					this.maxOccupation = this.occupation;
				}
				return true;
			}
		}
		
		public Double getMaxOccupation(){
			return this.maxOccupation;
		}
		
		public boolean unplug(){
			if(this.occupation == 0){
				return false;
			}
			return true;
		}
		
		public int getSlotIndex(){
			return this.slotIndex;
		}
		
		public Double getOccupation(){
			return this.occupation;
		}
		
		public Double getCap(){
			return this.cap;
		}
	}
}
