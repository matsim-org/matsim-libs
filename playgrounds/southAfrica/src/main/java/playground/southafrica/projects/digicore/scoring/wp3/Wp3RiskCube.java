/* *********************************************************************** *
 * project: org.matsim.*
 * Wp3RiskCube.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.southafrica.projects.digicore.scoring.wp3;

import playground.southafrica.projects.digicore.scoring.DigiScorer;
import playground.southafrica.projects.digicore.scoring.DigiScorer.RISK_GROUP;

/**
 * A class with only static methods to convert the Digicore RiskCube into 
 * different risk categories so that it is comparable to the different 
 * {@link DigiScorer} implementations.
 * 
 * @author jwjoubert
 */
public class Wp3RiskCube {

	/**
	 * Converts an accelerometer record into a specific speed bin. We use the 
	 * discretization as provided in the Work Package 3 proposal, Diagram 1.
	 * @param record
	 * @return
	 */
	public static int getRiskCubeSpeedBin(String record){
		int speedBin = 0;
		String[]sa = record.split(",");
		double speed = Double.parseDouble(sa[8]);
		if(speed <= 0.0){
			speedBin = 0;
		} else if(speed <= 5.0){
			speedBin = 1;
		} else if(speed <= 25.0){
			speedBin = 2;
		} else if(speed <= 45.0){
			speedBin = 3;
		} else if(speed <= 65.0){
			speedBin = 4;
		} else if(speed <= 80.0){
			speedBin = 5;
		} else if(speed <= 95.0){
			speedBin = 6;
		} else if(speed <= 110.0){
			speedBin = 7;
		} else if(speed <= 125.0){
			speedBin = 8;
		} else if(speed <= 140.0){
			speedBin = 9;
		} else if(speed <= 160.0){
			speedBin = 10;
		} else{
			speedBin = 11;
		}	
		return speedBin;
	}
	
	/**
	 * Converts an accelerometer record into a specific {@link DigiScorer.RISK_GROUP}. 
	 * We use the discretization as provided in the Work Package 3 proposal, 
	 * Diagram 1.
	 * 
	 * @param record
	 * @return
	 */
	public static RISK_GROUP getRiskCubeSpeedRisk(String record){
		int bin = getRiskCubeSpeedBin(record);
		switch (bin) {
		case 0:
		case 1:
			return RISK_GROUP.NONE;
		case 2:
		case 3:
			return RISK_GROUP.LOW;
		case 4:
		case 5:
		case 6:
			return RISK_GROUP.MEDIUM;
		case 7:
		case 8:
		case 9:
		case 10:
		case 11:
			return RISK_GROUP.HIGH;
		default:
			throw new RuntimeException("Don't know how to classify speed risk bin " + bin);
		}
	}
	
	/**
	 * Converts an accelerometer record into a specific longitudinal 
	 * acceleration bin. We use the discretization as provided in the Work 
	 * Package 3 proposal, Diagram 2.
	 * 
	 * @param record
	 * @return
	 */
	public static int getRiskcubeAccelXBin(String record){
		int accelXBin = 0;
		String[] sa = record.split(",");
		double accelX = Double.parseDouble(sa[5]);
		if(accelX < -600.0){
			accelXBin = 1;
		} else if(accelX < -500.0){
			accelXBin = 2;
		} else if(accelX < -425.0){
			accelXBin = 3;
		} else if(accelX < -350.0){
			accelXBin = 4;
		} else if(accelX < -310.0){
			accelXBin = 5;
		} else if(accelX < -275.0){
			accelXBin = 6;
		} else if(accelX < -240.0){
			accelXBin = 7;
		} else if(accelX < -205.0){
			accelXBin = 8;
		} else if(accelX < -170.0){
			accelXBin = 9;
		} else if(accelX < -135.0){
			accelXBin = 10;
		} else if(accelX < -100.0){
			accelXBin = 11;
		} else if(accelX < -50.0){
			accelXBin = 12;
		} else if(accelX < 50.0){
			accelXBin = 13;
		} else if(accelX < 100.0){
			accelXBin = 14;
		} else if(accelX < 135.0){
			accelXBin = 15;
		} else if(accelX < 170.0){
			accelXBin = 16;
		} else if(accelX < 205.0){
			accelXBin = 17;
		} else if(accelX < 240.0){
			accelXBin = 18;
		} else if(accelX < 275.0){
			accelXBin = 19;
		} else if(accelX < 310.0){
			accelXBin = 20;
		} else if(accelX < 350.0){
			accelXBin = 21;
		} else if(accelX < 425.0){
			accelXBin = 22;
		} else if(accelX < 500.0){
			accelXBin = 23;
		} else if(accelX < 600.0){
			accelXBin = 24;
		} else {
			accelXBin = 25;
		}
		return accelXBin;
	}
	
	/**
	 * Converts an accelerometer record into a specific lateral acceleration 
	 * bin. We use the discretization as provided in the Work Package 3 
	 * proposal, Diagram 2.
	 * 
	 * @param record
	 * @return
	 */
	public static int getRiskcubeAccelYBin(String record){
		int accelYBin = 0;
		String[] sa = record.split(",");
		double accelY = Double.parseDouble(sa[6]);
		if(accelY < -600.0){
			accelYBin = 1;
		} else if(accelY < -500.0){
			accelYBin = 2;
		} else if(accelY < -425.0){
			accelYBin = 3;
		} else if(accelY < -350.0){
			accelYBin = 4;
		} else if(accelY < -310.0){
			accelYBin = 5;
		} else if(accelY < -275.0){
			accelYBin = 6;
		} else if(accelY < -240.0){
			accelYBin = 7;
		} else if(accelY < -205.0){
			accelYBin = 8;
		} else if(accelY < -170.0){
			accelYBin = 9;
		} else if(accelY < -135.0){
			accelYBin = 10;
		} else if(accelY < -100.0){
			accelYBin = 11;
		} else if(accelY < -50.0){
			accelYBin = 12;
		} else if(accelY < 50.0){
			accelYBin = 13;
		} else if(accelY < 100.0){
			accelYBin = 14;
		} else if(accelY < 135.0){
			accelYBin = 15;
		} else if(accelY < 170.0){
			accelYBin = 16;
		} else if(accelY < 205.0){
			accelYBin = 17;
		} else if(accelY < 240.0){
			accelYBin = 18;
		} else if(accelY < 275.0){
			accelYBin = 19;
		} else if(accelY < 310.0){
			accelYBin = 20;
		} else if(accelY < 350.0){
			accelYBin = 21;
		} else if(accelY < 425.0){
			accelYBin = 22;
		} else if(accelY < 500.0){
			accelYBin = 23;
		} else if(accelY < 600.0){
			accelYBin = 24;
		} else {
			accelYBin = 25;
		}
		return accelYBin;
	}
	
	public static RISK_GROUP getRiskCubeAccelRisk(String record){
		int xBin = getRiskcubeAccelXBin(record);
		int yBin = getRiskcubeAccelYBin(record);
		
		switch (xBin) {
		case 1:
		case 2:
		case 24:
		case 25:
			return RISK_GROUP.HIGH;
		case 3:
		case 4:
		case 5:
		case 6:
		case 20:
		case 21:
		case 22:
		case 23:
			switch (yBin) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 21:
			case 22:
			case 23:
			case 24:
			case 25:
				return RISK_GROUP.HIGH;
			default:
				return RISK_GROUP.MEDIUM;
			}
		case 7:
		case 8:
		case 17:
		case 18:
		case 19:
			switch (yBin) {
			case 1:
			case 25:
				return RISK_GROUP.HIGH;
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 18:
			case 19:
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
				return RISK_GROUP.MEDIUM;
			default:
				return RISK_GROUP.LOW;
			}
		case 9:
		case 10:
		case 16:
			switch (yBin) {
			case 1:
			case 25:
				return RISK_GROUP.HIGH;
			case 2:
			case 3:
			case 4:
			case 5:
			case 21:
			case 22:
			case 23:
			case 24:
				return RISK_GROUP.MEDIUM;
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 16:
			case 17:
			case 18:
			case 19:
			case 20:
				return RISK_GROUP.LOW;
			default:
				return RISK_GROUP.NONE;
			}
		case 11:
		case 12:
		case 13:
		case 14:
		case 15:
			switch (yBin) {
			case 1:
			case 25:
				return RISK_GROUP.HIGH;
			case 2:
			case 3:
			case 4:
			case 5:
			case 21:
			case 22:
			case 23:
			case 24:
				return RISK_GROUP.MEDIUM;
			case 6:
			case 7:
			case 8:
			case 18:
			case 19:
			case 20:
				return RISK_GROUP.LOW;
			default:
				return RISK_GROUP.NONE;
			}
		default:
			break;
		}
		throw new RuntimeException("Don't know how to classify the following acceleration bins: x -> " + 
				xBin + "; y -> " + yBin);
	}
}
