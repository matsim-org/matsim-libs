package playground.fhuelsmann.emissions;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;




public class EmissionFactor  {
	private String outputPath;
	private  HbefaObject [] [] HbefaTable =
		new HbefaObject [21][4];
//	 double EF;	
//	 double emissions;
	
	
	public EmissionFactor(Map<String,Map<String, LinkedList<SingleEvent>>> map,HbefaObject [][] hbefaTable) {
		super();
		this.map = map;
		this.HbefaTable = hbefaTable;
	}

	

	Map<String,Map<String, LinkedList<SingleEvent>>> map = new TreeMap<String,Map<String, LinkedList<SingleEvent>>>();
	
	Map<String,Map<String, LinkedList<SingleEvent>>> finalMap = new TreeMap<String,Map<String, LinkedList<SingleEvent>>>();

	
	// Emission calculation based on average speed, EFh=Emission Factor heavy; EFf=Emission Factor freeflow; 
	// EFs= Emission Factor saturated; EFc=Emission Factor congested/stop&go; 
	// vh= average speed heavy; vf=average speedfreeflow; 
	// vs= average speed saturated; vc=average speed congested/stop&go;li = link length; vij= calcuated average speed from event file
	public static double emissionFactorCalculate(double vh,
			double vij,double vf,double EFh, double li,
			double EFs,double EFc,double EFf,double vs,double vc){
		
		double EF=0.0;
		double emissions= 0.0;
		
		
		if (vh <= vij && vij<=vf){
			double a = vf - vij;
			double b = vij-vh;
			 EF = (a *EFh ) / (a+b) + (b * EFf ) / (a+b);
			 emissions=EF*li;
			
		
		}
		else if (vs <= vij && vij<=vh){
			double a = vh - vij;
			double b = vij-vs;
			 EF = (a *EFs ) / (a+b) + (b * EFh ) / (a+b);
			 emissions=EF*li;
			
		}
		if (vc <= vij && vij<=vs){
			double a = vs - vij;
			double b = vij-vc;
			 EF = (a *EFc ) / (a+b) + (b * EFs ) / (a+b);
			 emissions=EF*li;
			
		
		}
		if (vij > vf){
			 EF = EFf;
			 emissions=EF*li;
		
		}
		else if(vij<vc){
			 EF =EFc;
			 emissions=EF;
			 emissions=EF*li;
			 
		}
		return emissions;
	}
	
	
	// Emission calculation based on stop&go and free flow fractions
	public static double emissionFreeFlowFractionCalculate(double vij,double li,
			double EFc,double EFf, int freeVelocity){
		
		double stopGoVel = 16.67;
	//	double freeFlowVel = 57.00; * in visumnetzlink1.txt the freeVelocity amounts to 60.00 km/h; lowest simulated average speed = 57.00 km/h
		double emissionsfraction =0.0;
		double freeFlowFraction =0.0;
		double stopGoFraction =0.0;
		double stopGoTime =0.0;
		double emissionfractions =0.0;
		
		
		stopGoTime= li/vij -li/freeVelocity;
//		System.out.print("\n Stop ang go time" + stopGoTime);
		stopGoFraction = stopGoVel *stopGoTime;
//		System.out.print("Stop ang go fraction" + stopGoFraction);
		freeFlowFraction= li - stopGoFraction;
		
		emissionfractions = stopGoFraction*	EFc + freeFlowFraction*	EFf;
		
		return emissionfractions;
	}
	

	public void createEmissionTables(){
			 
		for(Entry<String, Map<String, LinkedList<SingleEvent>>> LinkIdEntry : map.entrySet()){
			for (Iterator iter = LinkIdEntry.getValue().
				entrySet().iterator(); iter.hasNext();) {
 				Map.Entry entry = (Map.Entry) iter.next();
 				LinkedList value = (LinkedList)entry.getValue();	
		 					
		 	
		 				SingleEvent obj = (SingleEvent) value.pop();
		 			     
                        double emissionFractions;
		 				double emissionFactor;
                        

		 				if (obj.getHbefa_Road_type() ==0){
                        	 emissionFactor = 0.0;
                        	 emissionFractions= 0.0;
                           

                        }	 
		 				else{	
		 				double vfkm =HbefaTable[obj.getHbefa_Road_type()][0].getVelocity();
                        double EFh = HbefaTable[obj.getHbefa_Road_type()][0].getEmission_factor(); 

                        double vhkm =HbefaTable[obj.getHbefa_Road_type()][1].getVelocity();
                        double EFf = HbefaTable[obj.getHbefa_Road_type()][1].getEmission_factor();
                              

                        double vskm =HbefaTable[obj.getHbefa_Road_type()][2].getVelocity();
                        double EFs = HbefaTable[obj.getHbefa_Road_type()][2].getEmission_factor();

                        
                        double vckm =HbefaTable[obj.getHbefa_Road_type()][3].getVelocity();
                        double EFc = HbefaTable[obj.getHbefa_Road_type()][3].getEmission_factor();

                        double vij = obj.getAverageSpeed();
                 
                        double li = obj.getLinkLength();
                        int freeVelocity = obj.getfreeVelocity();
                        
                        emissionFactor=  emissionFactorCalculate(vhkm,vij,vfkm, EFh,li,
                        		EFs, EFc, EFf, vskm, vckm);
                        
                        emissionFractions=  emissionFreeFlowFractionCalculate(vij,li,
                    			 EFc, EFf,freeVelocity);
                     
                     //     System.out.println(emissionFactor);
                        		 			
		 				}
                                               
                        obj.setEmissionFactor(emissionFactor);
                        obj.setEmissionFractions(emissionFractions);
                        
		 				value.push(obj);
		 			//	map.entrySet.put(obj.getPersonal_id(), value);
		 			
		 				
		 		   }
		    }
	}
	
	
		public void printEmissionTable(){
			 String result="";
			
			for(Entry<String, Map<String, LinkedList<SingleEvent>>> LinkIdEntry : map.entrySet()){
				for (Iterator iter = LinkIdEntry.getValue().
					entrySet().iterator(); iter.hasNext();) {
	 				Map.Entry entry = (Map.Entry) iter.next();
	 				LinkedList value = (LinkedList)entry.getValue();	
	 					
	 			try{ 
	 				SingleEvent obj = (SingleEvent) value.pop();
	 						 				
	 				String activity = obj.getActivity();
	 				String travelTimeString = obj.getTravelTime();
	 				String enterTime = obj.getEnterTime();
	 				double v_mean = obj.getAverageSpeed();
	 				String Person_id = obj.getPersonal_id();
	 				int linkId_Veh_Id = obj.getLink_id();
	 				double length = obj.getLinkLength();
	 				int freeVelocity =obj.getfreeVelocity();
	 			
	 			System.out.println("\n"+activity 
	 				+"\nEnterTime :" + enterTime 	
					+"\nTravelTime :" + travelTimeString 
					+"\nAverageSpeed :" + v_mean
					+"\nLinkId : " + linkId_Veh_Id 
					+"\nPersonId :" + Person_id 
					+"\nLinklength : "+ length
					+"\nHbefaTypeNr : "+ obj.getHbefa_Road_type()
					+"\nRoadSection : " +  obj.getVisum_road_Section_Nr()
					+"\nVisumRoadTypeNr : " + obj.getVisumRoadType()
	 				+"\nEmissionsBasedOnAverageSpeed: " + obj.getEmissionFactor()
	 				+"\nEmissionsBasedOnFractions: " + obj.getEmissionFractions());
	 			
	 				result = result +"\n"
	 				+ enterTime
	 				+"\t" + travelTimeString 
					+"\t" + v_mean
					+"\t" + linkId_Veh_Id 
					+"\t" + Person_id 
					+"\t" + length
	 				+"\t" + obj.getHbefa_Road_type()
	//				+"\t" + obj.getVisum_road_Section_Nr()
					+"\t" + obj.getVisumRoadType()
					+"\t" + obj.getEmissionFactor()
					+"\t" + obj.getEmissionFractions();
				
	 						
	 				 
	 				}catch(Exception e){}
	 			}
		}
			try{
				  
			    // Create file 
			    FileWriter fstream = new FileWriter("../../detailedEval/teststrecke/sim/outputEmissions/out.txt");
			        BufferedWriter out = new BufferedWriter(fstream);
			        out.write("EnterTime \t travelTime \t AverageSpeed \t LinkId \t PersonId \tLinklength \tHbefaTypeNr \tVisumRoadTypeNr \t" + 
			        		  "EmissionsBasedOnAverageSpeed \t EmissionsBasedOnFractions" + result);
			    //Close the output stream
			    out.close();
					}catch (Exception e){//Catch exception if any
						System.err.println("Error: " + e.getMessage());
			    }
		}	
		
/*		public Map<String,Map<String, LinkedList<SingleEvent>>> getmap() {
			return map;
		}		*/
					
}