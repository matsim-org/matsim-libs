/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package noiseInternalization.version01;

//import org.apache.log4j.Logger;

public class GetAngle {
	
//	private static final Logger log = Logger.getLogger(GetAngle.class);

	public GetAngle() {
		
	}
	
	public static double getImmissionCorrection(double pointCoordX, double pointCoordY, double fromCoordX, double fromCoordY, double toCoordX, double toCoordY) {
		
		double immissionCorrection = 0.;
		
		double angle = 0.;
		
		double lotPointX = 0.;
		double lotPointY = 0.;
		
		double vectorX = toCoordX - fromCoordX;
		if(vectorX==0.) {
			vectorX=0.00000001;
			// dividing by zero is not possible
		}
		double vectorY = toCoordY - fromCoordY;
		
		double vector = vectorY/vectorX;
		if(vector==0.) {
			vector=0.00000001;
			// dividing by zero is not possible
		}
		
		double vector2 = (-1) * (1/vector);
		double yAbschnitt = fromCoordY - (fromCoordX*vector);
		double yAbschnittOriginal = fromCoordY - (fromCoordX*vector);
		
		double yAbschnitt2 = pointCoordY - (pointCoordX*vector2);
//		double yAbschnitt2Original = pointCoordY - (pointCoordX*vector2);
		
		double xValue = 0.;
		double yValue = 0.;
		
		if(yAbschnitt<yAbschnitt2) {
			yAbschnitt2 = yAbschnitt2 - yAbschnitt;
			yAbschnitt = 0;
			xValue = yAbschnitt2 / (vector - vector2);
			yValue = yAbschnittOriginal + (xValue*vector);
		} else if(yAbschnitt2<yAbschnitt) {
			yAbschnitt = yAbschnitt - yAbschnitt2;
			yAbschnitt2 = 0;
			xValue = yAbschnitt / (vector2 - vector);
			yValue = yAbschnittOriginal + (xValue*vector);
		}
		
		lotPointX = xValue;
		lotPointY = yValue;
		
		double angle1 = 0.;
		double angle2 = 0.;
		
		double kath1 = Math.abs(Math.sqrt(Math.pow(lotPointX - fromCoordX, 2) + Math.pow(lotPointY - fromCoordY, 2)));
		double hypo1 = Math.abs(Math.sqrt(Math.pow(pointCoordX - fromCoordX, 2) + Math.pow(pointCoordY - fromCoordY, 2)));
		double kath2 = Math.abs(Math.sqrt(Math.pow(lotPointX - toCoordX, 2) + Math.pow(lotPointY - toCoordY, 2)));
		double hypo2 = Math.abs(Math.sqrt(Math.pow(pointCoordX - toCoordX, 2) + Math.pow(pointCoordY - toCoordY, 2)));
		
		if(kath1==0) {
			kath1 = 0.0000001;
		}
		if(kath2==0) {
			kath2 = 0.0000001;
		}
		if(hypo1==0) {
			hypo1 = 0.0000001;
		}
		if(hypo2==0) {
			hypo2 = 0.0000001;
		}
		
		if(kath1>hypo1) {
			kath1 = hypo1;
		}
		if(kath2>hypo2) {
			kath2 = hypo2;
		}
		
		angle1 = Math.asin(kath1/hypo1);
		angle2 = Math.asin(kath2/hypo2);
		
		angle1 = Math.toDegrees(angle1);
		angle2 = Math.toDegrees(angle2);
		
		if((((fromCoordX<lotPointX)&&(toCoordX>lotPointX))||((fromCoordX>lotPointX)&&(toCoordX<lotPointX)))||(((fromCoordY<lotPointY)&&(toCoordY>lotPointY))||((fromCoordY>lotPointY)&&(toCoordY<lotPointY)))) {
			angle = Math.abs(angle1 + angle2);
		} else {
			angle = Math.abs(angle1 - angle2);
		}
		
		immissionCorrection = 10*Math.log10((angle)/(180));
		
		return immissionCorrection;
	}

}
