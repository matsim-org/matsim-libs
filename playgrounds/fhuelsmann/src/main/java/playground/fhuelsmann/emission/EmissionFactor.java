/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */

package playground.fhuelsmann.emission;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.matsim.api.core.v01.Id;


public class EmissionFactor  {
	
	private  HbefaObject [] [] HbefaTable =
		new HbefaObject [21][4];
	
	
	public EmissionFactor(Map<Id,Map<Id, LinkedList<SingleEvent>>> map,HbefaObject [][] hbefaTable) {
		super();
		this.map = map;
		this.HbefaTable = hbefaTable;
	}

	Map<Id,Map<Id, LinkedList<SingleEvent>>> map = new TreeMap<Id,Map<Id, LinkedList<SingleEvent>>>();
	
	
	// Emission calculation based on average speed, EFh=Emission Factor heavy; EFf=Emission Factor freeflow; 
	// EFs= Emission Factor saturated; EFc=Emission Factor congested/stop&go; 
	// vh= average speed heavy; vf=average speedfreeflow; 
	// vs= average speed saturated; vc=average speed congested/stop&go;li = link length; vij= calcuated average speed from event file
	public static double [] emissionFactorCalculate(double vh,
			double vij,double vf,double EFh, double li,
			double EFs,double EFc,double EFf,double vs,double vc, double noxf, double noxh, double noxs, double noxc){
		
		double EF=0.0;
		double emissions= 0.0;
		double nox =0.0;
		double noxEmissions=0.0;
		
		
		if (vh <= vij && vij<=vf){
			double a = vf - vij;
			double b = vij-vh;
			 EF = (a *EFh ) / (a+b) + (b * EFf ) / (a+b);
			 emissions=EF*(li/1000);
			 nox = (a *noxh ) / (a+b) + (b * noxf ) / (a+b);
			 noxEmissions=nox*(li/1000);
	//		 System.out.print("link length fast" + li);
		
		}
		else if (vs <= vij && vij<=vh){
			double a = vh - vij;
			double b = vij-vs;
			 EF = (a *EFs ) / (a+b) + (b * EFh ) / (a+b);
			 emissions=EF*(li/1000);
			 nox = (a *noxs ) / (a+b) + (b * noxh ) / (a+b);
			 noxEmissions=nox*(li/1000);
	//		 System.out.print("link length heavy" + li);
		}
		if (vc <= vij && vij<=vs){
			double a = vs - vij;
			double b = vij-vc;
			 EF = (a *EFc ) / (a+b) + (b * EFs ) / (a+b);
			 emissions=EF*(li/1000);
			 nox = (a *noxc ) / (a+b) + (b * noxs ) / (a+b);
			 noxEmissions=nox*(li/1000);
	//		 System.out.print("link length slow" + li);
		
		}
		if (vij > vf){
			 EF = EFf;
			 emissions=EF*(li/1000);
			 nox = noxf;
			 noxEmissions=nox*(li/1000);
	//		 System.out.print("link length very fast" + li);
		}
		else if(vij<vc){
			 EF =EFc;
			 emissions=EF*(li/1000);
			 nox =noxc;
			 noxEmissions=nox*(li/1000);
	//		 System.out.print("link length very slow" + li);
		}
		double [] result = new double[3];
		result[0]=EF;
		result[1]=emissions;
		result[2]=noxEmissions;
		
		return  result;
	}
	
	
	// Emission calculation based on stop&go and free flow fractions
	public static double [] emissionFreeFlowFractionCalculate(double vij,double li,
			double EFc,double EFf,double vf, double noxf, double noxc /*,int freeVelocity*/){
		
		double stopGoVel = 12.7567;
	//	in visumnetzlink1.txt the freeVelocity is 60.00 km/h;  average free flow speed in HBEFA = 57.1577 km/h which is taken here
	
		double freeFlowFraction =0.0;
		double stopGoFraction =0.0;
		double stopGoTime =0.0;
		double emissionsfractions =0.0;
		double noxFractions=0.0;
		
		
		if (vij<stopGoVel){
			emissionsfractions = li/1000*EFc;
			noxFractions=  li/1000*	noxc;
			
			}
		
		else {
		stopGoTime= (li/1000)/vij -(li/1000)/vf;  //li/vij -li/freeVelocity;
	
		stopGoFraction = stopGoVel *stopGoTime;
		freeFlowFraction= (li/1000) - stopGoFraction;
		
		emissionsfractions = stopGoFraction*	EFc + freeFlowFraction*	EFf;
		noxFractions=  stopGoFraction*	noxc + freeFlowFraction*	noxf;
		}
		
		double [] fraction = new double[2];
		fraction[0] =emissionsfractions;
		fraction[1] =noxFractions;
		return fraction;
	}
	

	public void createEmissionTables(){
		 String result="";
		for(Entry<Id, Map<Id, LinkedList<SingleEvent>>> LinkIdEntry : map.entrySet()){
			for (Iterator iter = LinkIdEntry.getValue().
				entrySet().iterator(); iter.hasNext();) {
 				Map.Entry entry = (Map.Entry) iter.next();
 				LinkedList value = (LinkedList)entry.getValue();	
		 					
 						
 				
 				//create object from SingleEvent, object is of type SingleEvent, when class is called it will get an instance
		 				SingleEvent obj = (SingleEvent) value.pop();
		 			     
                        double emissionFractions;
		 				double emissionFactor;
		 				double emissions;
		 				double noxEmissions;
		 				double noxFractions;
                        

		 				if (obj.getHbefa_Road_type() ==0){
                        	 emissionFactor = 0.0;
                        	 emissions = 0.0;
                        	 emissionFractions= 0.0;
                        	 noxFractions =0.0;
                        	 noxEmissions =0.0;
                           

                        }	 
		 				else{	
		 				double vf =HbefaTable[obj.getHbefa_Road_type()][0].getVelocity();
                        double EFf = HbefaTable[obj.getHbefa_Road_type()][0].getEmission_factor(); 
                        double noxf = HbefaTable[obj.getHbefa_Road_type()][0].getEmissionFactorNox();

                        double vh =HbefaTable[obj.getHbefa_Road_type()][1].getVelocity();
                        double EFh = HbefaTable[obj.getHbefa_Road_type()][1].getEmission_factor();
                        double noxh = HbefaTable[obj.getHbefa_Road_type()][1].getEmissionFactorNox();
                              

                        double vs =HbefaTable[obj.getHbefa_Road_type()][2].getVelocity();
                        double EFs = HbefaTable[obj.getHbefa_Road_type()][2].getEmission_factor();
                        double noxs = HbefaTable[obj.getHbefa_Road_type()][2].getEmissionFactorNox();

                        
                        double vc =HbefaTable[obj.getHbefa_Road_type()][3].getVelocity();
                        double EFc = HbefaTable[obj.getHbefa_Road_type()][3].getEmission_factor();
                        double noxc = HbefaTable[obj.getHbefa_Road_type()][3].getEmissionFactorNox();

                        double vij = obj.getAverageSpeed();
                 
                        double li = obj.getLinkLength();
                  //      int freeVelocity = obj.getfreeVelocity();
                        
                        //call of function, double output
                         double [] emissionFactorAndEmissions=  emissionFactorCalculate(vh,vij,vf, EFh,li,
                        		EFs, EFc, EFf, vs, vc, noxf, noxh,noxs, noxc);
                         emissionFactor = emissionFactorAndEmissions[0];
                         obj.setEmissionFactor(emissionFactor);
                         emissions = emissionFactorAndEmissions[1];
                         obj.setEmissions(emissions);
                         noxEmissions = emissionFactorAndEmissions[2];
                         obj.setNoxEmissions(noxEmissions);
                         
                        
                         //call of function double output
                         double [] fractions=  emissionFreeFlowFractionCalculate(vij,li,
                    			 EFc, EFf,/*freeVelocity,*/vf, noxf, noxc);
                         emissionFractions =fractions[0];
                         obj.setEmissionFractions(emissionFractions);
                         noxFractions =fractions [1];
                         obj.setNoxFractions (noxFractions);
                      		
		 				 }
                        
		 				value.push(obj);
		 			//	map.entrySet.put(obj.getPersonal_id(), value);
		 				
		 				
		 				String activity = obj.getActivity();
		 				String travelTimeString = obj.getTravelTime();
		 				String enterTime = obj.getEnterTime();
		 				double v_mean = obj.getAverageSpeed();
		 				String Person_id = obj.getPersonal_id();
		 				String Link_id = obj.getLink_id();
		 				double length = obj.getLinkLength();
		 				int freeVelocity =obj.getfreeVelocity();
		 				
		 				result = result +"\n"
		 				+ enterTime
		 				+"\t" + travelTimeString 
						+"\t" + v_mean
						+"\t" + Link_id 
						+"\t" + Person_id 
						+"\t" + length
		 				+"\t" + obj.getHbefa_Road_type()
		//				+"\t" + obj.getVisum_road_Section_Nr()
						+"\t" + obj.getVisumRoadType()
						+"\t" + obj.getEmissionFactor()
						+"\t" + obj.getNoxEmissions()
						+"\t" + obj.getEmissions()
						+"\t" + obj.getEmissionFractions()
		 				+"\t" + obj.getNoxFractions();
		 				
		 				System.out.print("\n LinkID " + Link_id + "PersonID " + Person_id);
		 				
		 				try {
		 					  
		 				    // Create file 
		 				    FileWriter fstream = new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/out.txt");
		 				        BufferedWriter out = new BufferedWriter(fstream);
		 				        out.write("EnterTime \t travelTime \t AverageSpeed \t LinkId \t PersonId \tLinklength \tHbefaTypeNr \tVisumRoadTypeNr \t" + 
		 				        		"MKrEmissionsFactorBasedOnAverageSpeed \t NoxEmissionsBasedOnAverageSpeed \t " +
		 				        		"MKrBasedOnAverageSpeed \t MKrBasedOnFractions \t NoxEmissionsBasedOnFractions" + result);
		 				    //Close the output stream
		 				    out.close();
		 						}catch (Exception e){//Catch exception if any
		 							System.err.println("Error: " + e.getMessage());
		 				    }
		 				 												 				
		 		   }
		    }
	}
	
	
	/*	public void printEmissionTable(){
			 String result="";
			
			for(Entry<Id, Map<Id, LinkedList<SingleEvent>>> LinkIdEntry : map.entrySet()){
				for (Iterator iter = LinkIdEntry.getValue().
					entrySet().iterator(); iter.hasNext();) {
	 				Map.Entry entry = (Map.Entry) iter.next();
	 				LinkedList value = (LinkedList)entry.getValue();	
	 					
	 			try{ 
	 				//pop is a procedure, if used the value is deleted in the list, peek could be used as well	 				
	 				SingleEvent obj = (SingleEvent) value.pop();
	 						 				
	 				String activity = obj.getActivity();
	 				String travelTimeString = obj.getTravelTime();
	 				String enterTime = obj.getEnterTime();
	 				double v_mean = obj.getAverageSpeed();
	 				String Person_id = obj.getPersonal_id();
	 				String Link_id = obj.getLink_id();
	 				double length = obj.getLinkLength();
	 				int freeVelocity =obj.getfreeVelocity();
	 			
	 			System.out.println("\n"+activity 
	 				+"\nEnterTime :" + enterTime 	
					+"\nTravelTime :" + travelTimeString 
					+"\nAverageSpeed :" + v_mean
					+"\nLinkId : " + Link_id 
					+"\nPersonId :" + Person_id 
					+"\nLinklength : "+ length
					+"\nHbefaTypeNr : "+ obj.getHbefa_Road_type()
					+"\nVisumRoadTypeNr : " + obj.getVisumRoadType()
					+"\nMKrEmissionsFactorBasedOnAverageSpeed: " + obj.getEmissionFactor()
					+"\nNoxEmissionsBasedOnAverageSpeed: " + obj.getNoxEmissions()
	 				+"\nMKrBasedOnAverageSpeed: " + obj.getEmissions()
	 				+"\nMKrBasedOnFractions: " + obj.getEmissionFractions()
	 				+"\nNoxEmissionsBasedOnFractions: " + obj.getNoxFractions());
	 			
	 				result = result +"\n"
	 				+ enterTime
	 				+"\t" + travelTimeString 
					+"\t" + v_mean
					+"\t" + Link_id 
					+"\t" + Person_id 
					+"\t" + length
	 				+"\t" + obj.getHbefa_Road_type()
	//				+"\t" + obj.getVisum_road_Section_Nr()
					+"\t" + obj.getVisumRoadType()
					+"\t" + obj.getEmissionFactor()
					+"\t" + obj.getNoxEmissions()
					+"\t" + obj.getEmissions()
					+"\t" + obj.getEmissionFractions()
	 				+"\t" + obj.getNoxFractions();
					 										 
	 				}catch(Exception e){}
	 			}
		}
			try {
				  
			    // Create file 
			    FileWriter fstream = new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/out_all.txt");
			        BufferedWriter out = new BufferedWriter(fstream);
			        out.write("EnterTime \t travelTime \t AverageSpeed \t LinkId \t PersonId \tLinklength \tHbefaTypeNr \tVisumRoadTypeNr \t" + 
			        		"MKrEmissionsFactorBasedOnAverageSpeed \t NoxEmissionsFactorBasedOnAverageSpeed \t " +
			        		"MKrBasedOnAverageSpeed \t MKrBasedOnFractions \t NoxEmissionsBasedOnFractions" + result);
			    //Close the output stream
			    out.close();
					}catch (Exception e){//Catch exception if any
						System.err.println("Error: " + e.getMessage());
			    }
		}	*/
		
/*		public Map<String,Map<String, LinkedList<SingleEvent>>> getmap() {
			return map;
		}		*/
					
}