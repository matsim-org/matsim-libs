package playground.artemc.smartCardDataTools.zoneMerger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;



public class ZoneMerger {
	
	HashMap<Integer, Zone> zoneMap = new HashMap<Integer, Zone>(); 
	HashMap<Integer, Integer> zoneAssignment = new HashMap<Integer, Integer>(); 
//	ArrayList<Zone> zoneList = new ArrayList<Zone>();
	int oCount=0;
	boolean containsLowObservations=true;
	
	
	public static void main(String args[]){
		new ZoneMerger().go();
	}

	private void go(){
		
		for(int i=0;i<1092;i++){
			zoneAssignment.put(i,i);
		}
		
		getZones();
		getHITSSamples();	
		
		//Iterate for zones beginning with lowest number of observations > 0
		while(oCount<50){
			oCount++;
			boolean containsLowObservations=true;
			System.out.println(oCount);
			//Iterate over zones as long as for all zones: observations > oCount
			while(containsLowObservations){
				for(Integer i: zoneMap.keySet()){
//					System.out.println(i);
						if(zoneMap.get(i).getObservations()==oCount){							
							int minObservations=-1;
							int zoneToMerge=-1;
							int newZoneNumber;
							int newObservations=0;
							String newAdjacentZones="";
							containsLowObservations=true;
							
							//Find adjacent zone with lowest number of observations	(also 0)										
							for(int az:zoneMap.get(i).getAdjacentZones()){								
								int azNew = zoneAssignment.get(az); 
								if(azNew!=zoneMap.get(i).getZoneNumber()){
									if((zoneMap.get(azNew).getObservations()<=minObservations || minObservations==-1) && zoneMap.get(azNew).getObservations()>0){
										minObservations=zoneMap.get(azNew).getObservations();
										zoneToMerge = azNew;
									}
									newAdjacentZones+=az+";";
								}
							}
							
							
							//Create a new zone out of old 2
							if(zoneToMerge!=-1){
								//Add adjacent zones
								for(int az:zoneMap.get(zoneToMerge).getAdjacentZones()){
									newAdjacentZones+=az+";"; 
								}
										
								if(zoneMap.get(i).getZoneNumber()<=zoneMap.get(zoneToMerge).getZoneNumber()){
									newZoneNumber=zoneMap.get(i).getZoneNumber();
								}
								else{
									newZoneNumber=zoneMap.get(zoneToMerge).getZoneNumber();
								}
//								System.out.println(i+","+zoneToMerge+"   "+newZoneNumber);
								newObservations = zoneMap.get(i).getObservations()+zoneMap.get(zoneToMerge).getObservations();
					
								Zone mergedZone = new Zone(newZoneNumber+","+newAdjacentZones);
								mergedZone.setObservations(newObservations);
//								System.out.println(mergedZone.zoneNumber+","+mergedZone.adjacentZones+","+mergedZone.observations);
								for(Integer k: zoneAssignment.keySet()){
									if(zoneAssignment.get(k).equals(zoneMap.get(i).getZoneNumber()) || zoneAssignment.get(k).equals(zoneMap.get(zoneToMerge).getZoneNumber()))
										zoneAssignment.put(k, newZoneNumber);
								}
								
								zoneMap.remove(i);
								zoneMap.remove(zoneToMerge);
								zoneMap.put(newZoneNumber, mergedZone);
								System.out.println(zoneMap.size());
								break;
							} 
			
						}
						else{
							containsLowObservations=false;
						} //end of if for number of observations in the zone
				} //end of for-loop through the zones (only till first merge is found)
			}//end of while-loop through the zones

		} //end of while loop for each minimal number of observations (oCount)	
		System.out.println(zoneAssignment);
		
		//Write the new zone assignment to file
		writeZoneAssignment(zoneAssignment, zoneMap);
	}

	private void writeZoneAssignment(HashMap<Integer,Integer> zoneAssignment, HashMap<Integer,Zone> zoneMap) {
		File file = new File("./data/zoneMerge/zoneAssignment.csv");
		BufferedWriter out=null;
		try {
			out = new BufferedWriter(new FileWriter(file));
			for(Integer k: zoneAssignment.keySet()){
				out.write(Integer.toString(k)+","+Integer.toString(zoneAssignment.get(k))+","+Integer.toString(zoneMap.get(zoneAssignment.get(k)).getObservations())+"\n");
				out.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {                      
	    	if (out!= null) try { 
	    		 out.close();
	    		 } catch (IOException ioe2) {
	    		 }
	     } 	
	}

	private void getZones(){
		File file = new File("./data/zoneMerge/mtz1092p_DGP_Adjacent.txt");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			int count=0;
			while((line=reader.readLine())!=null){
				Zone nextZone = new Zone(line);
				zoneMap.put(count, nextZone);
				count++;
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void getHITSSamples() {
		File file = new File("./data/zoneMerge/ObservationsPerZone1092.csv");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = null;
			int count=0;
			while((line=reader.readLine())!=null){
				zoneMap.get(count).setObservations(Integer.parseInt(line));
				count++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}





	
}
