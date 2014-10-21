/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.ptTripAnalysis;

import org.apache.log4j.Logger;

/**
 * 
 * Simple container holding the data of one file. In addition, calculates most common averages.
 * 
 * @author aneumann
 *
 */
public class PtTripAnalysisContainer {
	
	private static final Logger log = Logger.getLogger(PtTripAnalysisContainer.class);
	
	public final static int INSIDE_ZONE = 0;
	public final static int LEAVING_ZONE = 1;
	public final static int ENTERING_ZONE = 2;
	public final static int OUTSIDE_ZONE = 3;
	
	private double[] sumTravelTime = null;
	private int[] tripCnt = null;
	private int[] accesWalkCnt = null;
	private int[] accesWaitCnt = null;
	private int[] egressWalkCnt = null;
	private int[] switchWalkCnt = null;
	private int[] switchWaitCnt = null;
	private int[] lineCnt = null;
	private double[] accesWalkTTime = null;
	private double[] accesWaitTime = null;
	private double[] egressWalkTTime = null;
	private double[] switchWalkTTime = null;
	private double[] switchWaitTime = null;
	private double[] lineTTime = null;
	private int[] line1cnt = null;
	private int[] line2cnt = null;
	private int[] line3cnt = null;
	private int[] line4cnt = null;
	private int[] line5cnt = null;
	private int[] line6cnt = null;
	private int[] line7cnt = null;
	private int[] line8cnt = null;
	private int[] line9cnt = null;
	private int[] line10cnt = null;
	private int[] lineGt10cnt = null;
	
	
	public double[] getSumTravelTime() {
		return this.sumTravelTime;
	}
	public void setSumTravelTime(double[] sumTravelTime) {
		if (this.sumTravelTime == null) {
			this.sumTravelTime = sumTravelTime;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getTripCnt() {
		return this.tripCnt;
	}
	public void setTripCnt(int[] tripCnt) {
		if (this.tripCnt == null) {
			this.tripCnt = tripCnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getAccesWalkCnt() {
		return this.accesWalkCnt;
	}
	public void setAccesWalkCnt(int[] accesWalkCnt) {
		if (this.accesWalkCnt == null) {
			this.accesWalkCnt = accesWalkCnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getAccesWaitCnt() {
		return this.accesWaitCnt;
	}
	public void setAccesWaitCnt(int[] accesWaitCnt) {
		if (this.accesWaitCnt == null) {
			this.accesWaitCnt = accesWaitCnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getEgressWalkCnt() {
		return this.egressWalkCnt;
	}
	public void setEgressWalkCnt(int[] egressWalkCnt) {
		if (this.egressWalkCnt == null) {
			this.egressWalkCnt = egressWalkCnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getSwitchWalkCnt() {
		return this.switchWalkCnt;
	}
	public void setSwitchWalkCnt(int[] switchWalkCnt) {
		if (this.switchWalkCnt == null) {
			this.switchWalkCnt = switchWalkCnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getSwitchWaitCnt() {
		return this.switchWaitCnt;
	}
	public void setSwitchWaitCnt(int[] switchWaitCnt) {
		if (this.switchWaitCnt == null) {
			this.switchWaitCnt = switchWaitCnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLineCnt() {
		return this.lineCnt;
	}
	public void setLineCnt(int[] lineCnt) {
		if (this.lineCnt == null) {
			this.lineCnt = lineCnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public double[] getAccesWalkTTime() {
		return this.accesWalkTTime;
	}
	public void setAccesWalkTTime(double[] accesWalkTTime) {
		if (this.accesWalkTTime == null) {
			this.accesWalkTTime = accesWalkTTime;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public double[] getAccesWaitTime() {
		return this.accesWaitTime;
	}
	public void setAccesWaitTime(double[] accesWaitTime) {
		if (this.accesWaitTime == null) {
			this.accesWaitTime = accesWaitTime;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public double[] getEgressWalkTTime() {
		return this.egressWalkTTime;
	}
	public void setEgressWalkTTime(double[] egressWalkTTime) {
		if (this.egressWalkTTime == null) {
			this.egressWalkTTime = egressWalkTTime;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public double[] getSwitchWalkTTime() {
		return this.switchWalkTTime;
	}
	public void setSwitchWalkTTime(double[] switchWalkTTime) {
		if (this.switchWalkTTime == null) {
			this.switchWalkTTime = switchWalkTTime;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public double[] getSwitchWaitTime() {
		return this.switchWaitTime;
	}
	public void setSwitchWaitTime(double[] switchWaitTime) {
		if (this.switchWaitTime == null) {
			this.switchWaitTime = switchWaitTime;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public double[] getLineTTime() {
		return this.lineTTime;
	}
	public void setLineTTime(double[] lineTTime) {
		if (this.lineTTime == null) {
			this.lineTTime = lineTTime;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine1cnt() {
		return this.line1cnt;
	}
	public void setLine1cnt(int[] line1cnt) {
		if (this.line1cnt == null) {
			this.line1cnt = line1cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine2cnt() {
		return this.line2cnt;
	}
	public void setLine2cnt(int[] line2cnt) {
		if (this.line2cnt == null) {
			this.line2cnt = line2cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine3cnt() {
		return this.line3cnt;
	}
	public void setLine3cnt(int[] line3cnt) {
		if (this.line3cnt == null) {
			this.line3cnt = line3cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine4cnt() {
		return this.line4cnt;
	}
	public void setLine4cnt(int[] line4cnt) {
		if (this.line4cnt == null) {
			this.line4cnt = line4cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine5cnt() {
		return this.line5cnt;
	}
	public void setLine5cnt(int[] line5cnt) {
		if (this.line5cnt == null) {
			this.line5cnt = line5cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine6cnt() {
		return this.line6cnt;
	}
	public void setLine6cnt(int[] line6cnt) {
		if (this.line6cnt == null) {
			this.line6cnt = line6cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine7cnt() {
		return this.line7cnt;
	}
	public void setLine7cnt(int[] line7cnt) {
		if (this.line7cnt == null) {
			this.line7cnt = line7cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine8cnt() {
		return this.line8cnt;
	}
	public void setLine8cnt(int[] line8cnt) {
		if (this.line8cnt == null) {
			this.line8cnt = line8cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine9cnt() {
		return this.line9cnt;
	}
	public void setLine9cnt(int[] line9cnt) {
		if (this.line9cnt == null) {
			this.line9cnt = line9cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLine10cnt() {
		return this.line10cnt;
	}
	public void setLine10cnt(int[] line10cnt) {
		if (this.line10cnt == null) {
			this.line10cnt = line10cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	public int[] getLineGt10cnt() {
		return this.lineGt10cnt;
	}
	public void setLineGt10cnt(int[] lineGt10cnt) {
		if (this.lineGt10cnt == null) {
			this.lineGt10cnt = lineGt10cnt;
		} else {
			log.error("Reading one object twice.");
		}
	}
	
	

}
