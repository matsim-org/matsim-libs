/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.nmviljoen.network;

import java.io.FileNotFoundException;

public class MaritimeDirectedNetwork {

	public static void main(String[] args) throws FileNotFoundException{
		
	}
	class MyNode {
		String intID;
		String id;
		double X;
		double Y;
		public MyNode(String intID, String id, double X, double Y) {
			this.intID = intID;
			this.id = id;
			this.X =X;
			this.Y=Y;
		}
		public String toString() {
			return intID +" "+ id+" "+X+" "+Y+'\n';
		}        
		public String getId(){
			return id;
		}
		public String getX(){
			return Double.toString(X);
		}
		public String getY(){
			return Double.toString(Y);
		}
	}

	class MyLink {
		String id;
		double weight;//capacity in this case
		double transProb;
		public MyLink(String id, double weight, double transProb) {
			this.id = id;
			this.weight = weight;
			this.transProb = -99;
		} 
		public String getId(){
			return id;
		}
		public double getWeight(){
			return weight;
		}
		public void setTransProb(double newTransProb){
			transProb = newTransProb;
		}
		public double getTransProb(){
			return transProb;
		}
		public String toString() {
			return id+" "+weight+'\n';
		}
		

	}
}
