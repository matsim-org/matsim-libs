/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips.distance;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

import playground.droeder.Analysis.Trips.AbstractAnalysisTrip;
import playground.droeder.Analysis.Trips.AbstractAnalysisTripSet;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class DistanceAnalysisTripSet extends AbstractAnalysisTripSet{
	private static final Logger log = Logger
			.getLogger(DistanceAnalysisTripSet.class);
	
	//[0]inside, [1]leaving Zone, [2]entering Zone, [3] outSide
	private Integer[] tripCnt= null;
	private Double[] tripDist= null;
	
	private Double[] accesWalkDist= null;
	private Integer[] accesWalkCnt= null;

	private Double[] inPtDist= null;
	private Integer[] lineCnt= null;
	
	private Double[] egressWalkDist= null;
	private Integer[] egressWalkCnt= null;

	private Double[] switchWalkDist= null;
	private Integer[] switchWalkCnt= null;
	
	
	
	public DistanceAnalysisTripSet(String mode, Geometry zone){
		super(mode, zone);
		this.init();
	}

	private void init() {
		this.tripDist = new Double[4];
		this.tripCnt = new Integer[4];
		if(super.getMode().equals(TransportMode.pt)){
			this.accesWalkDist = new Double[4];
			this.accesWalkCnt = new Integer[4];
			this.egressWalkDist = new Double[4];
			this.egressWalkCnt = new Integer[4];
			this.inPtDist = new Double[4];
			this.lineCnt = new Integer[4];
			this.switchWalkDist = new Double[4];
			this.switchWalkCnt = new Integer[4];
		}
		
		for(int i = 0; i < 4; i++){
			this.tripDist[i] = new Double(0.0);
			this.tripCnt[i] = new Integer(0);
			if(super.getMode().equals(TransportMode.pt)){
				this.accesWalkDist[i] = new Double(0.0);
				this.accesWalkCnt[i] = new Integer(0);
				this.egressWalkDist[i] = new Double(0.0);
				this.egressWalkCnt[i] = new Integer(0);
				this.inPtDist[i] = new Double(0.0);
				this.lineCnt[i] = new Integer(0);
				this.switchWalkDist[i] = new Double(0.0);
				this.switchWalkCnt[i] = new Integer(0);
			}
		}
	}

	@Override
	protected void addTripValues(AbstractAnalysisTrip trip) {
		if(!(trip instanceof DistAnalysisTripI)){
			log.error("given Trip has the wrong type and is not processed!"); 
			return;
		}
		
		int location = super.getTripLocation(trip);
		this.tripCnt[location]++;
		DistAnalysisTrip t = (DistAnalysisTrip) trip;
		this.tripDist[location] += t.getTripDist();

		if(super.getMode().equals(TransportMode.pt)){
			this.accesWalkCnt[location] += t.getAccesWalkCnt();
			this.accesWalkDist[location] += t.getAccesWalkDist();
			
			this.inPtDist[location] += t.getInPtDist();
			this.lineCnt[location] += t.getLineCnt();
			
			this.switchWalkCnt[location] += t.getSwitchWalkCnt();
			this.switchWalkDist[location] += t.getSwitchWalkDist();

			this.egressWalkCnt[location] += t.getEgressWalkCnt();
			this.egressWalkDist[location] += t.getEgressWalkDist();
		}
	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		//[0]inside, [1]leaving Zone, [2]entering Zone, [3] outSide
		//print header
		b.append(";inside Zone;leaving Zone;entering Zone;outside Zone; \n");
		
		b.append("tripCnt;"); super.println(this.tripCnt, b);
		b.append("tripDist [m];"); super.println(this.tripDist, b);
		
		if(super.getMode().equals(TransportMode.pt)){
			b.append("accesWalkCnt;"); super.println(this.accesWalkCnt, b);
			b.append("lineCnt;"); super.println(this.lineCnt, b);
			b.append("switchWalkCnt;"); super.println(this.switchWalkCnt, b);
			b.append("egressWalkCnt;"); super.println(this.egressWalkCnt, b);
			
			b.append("accesWalkDist [m];"); super.println(this.accesWalkDist, b);
			b.append("inPtDist [m];"); super.println(this.inPtDist, b);
			b.append("switchWalkDist [m];"); super.println(this.switchWalkDist, b);
			b.append("egressWalkDist [m];"); super.println(this.egressWalkDist, b);
		}
		return b.toString();
	}

}
