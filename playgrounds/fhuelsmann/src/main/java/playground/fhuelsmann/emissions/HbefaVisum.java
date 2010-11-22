package playground.fhuelsmann.emissions;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class HbefaVisum {

	private final VisumObject[] roadTypes = new VisumObject[100];
	Map<String,Map<String, LinkedList<SingleEvent>>> map = new  
		TreeMap<String,Map<String, LinkedList<SingleEvent>>>();
	
	public HbefaVisum(Map<String,Map<String, LinkedList<SingleEvent>>> map) {
		super();
		this.map = map;
	}

	public void creatRoadTypes(String filename){

			try{
	
				FileInputStream fstream = new FileInputStream(filename);
			    // Get the object of DataInputStream
			    DataInputStream in = new DataInputStream(fstream);
		        BufferedReader br = new BufferedReader(new InputStreamReader(in));
			    String strLine;
			    //Read File Line By Line
			    br.readLine();
				    while ((strLine = br.readLine()) != null)   {
				    	//for all lines (whole text) we split the line to an array 
				    
				       	String[] array = strLine.split(",");
				    	
				    	VisumObject obj = new VisumObject(Integer.parseInt(array[0]), array[2]);
				    	this.roadTypes[obj.getVISUM_RT_NR()] = obj;
				    }
				    //Close the input stream
				    in.close();
				    }catch (Exception e){//Catch exception if any
				      System.err.println("Error: " + e.getMessage());
				    }
		}
		
	public void printHbefaVisum(){
			
			for(int i=0;i<100; i++){
		
				VisumObject obj = roadTypes[i];
				
				System.out.println("\nVISUM_RT_NR :" + obj.getVISUM_RT_NR() +
									"\nHBEFA_RT_NR :" + obj.getHBEFA_RT_NR() );								
			}
	}

	public  String findHbefaFromVisum(int VisumRoadTypeNumber){
		return this.roadTypes[VisumRoadTypeNumber].getHBEFA_RT_NR();
	}
	
	
	public void createMapWithHbefaRoadTypeNumber(){

	for(Entry<String, Map<String, LinkedList<SingleEvent>>> LinkIdEntry : this.map.entrySet()){	 
		 for (Iterator iter = LinkIdEntry.getValue().
	    			entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
 				LinkedList value = (LinkedList)entry.getValue();	
		 				
		 			SingleEvent singleEvent = (SingleEvent) value.pop();
		 			HbefaTable hbefaTable = new HbefaTable();
		 			String a = findHbefaFromVisum(singleEvent.getVisumRoadType());
		 			singleEvent.setHbefa_Road_type(Integer.parseInt(a));
		 			value.push(singleEvent);
				
			}
			}}
	
	public void printTable(){
	
		
		
		
		 
		for(Entry<String, Map<String, LinkedList<SingleEvent>>> LinkIdEntry : this.map.entrySet()){	 
			 for (Iterator iter = LinkIdEntry.getValue().
		    			entrySet().iterator(); iter.hasNext();) {
					Map.Entry entry = (Map.Entry) iter.next();
	 				LinkedList value = (LinkedList)entry.getValue();
	 				
		 		try{ 
		 				SingleEvent obj = (SingleEvent) value.pop();
		 						 				
		 				String activity = obj.getActivity();
		 				String travelTimeString = obj.getTravelTime();
		 				double v_mean = obj.getAverageSpeed();
		 				String Person_id = obj.getPersonal_id();
		 				int linkId_Veh_Id = obj.getLink_id();
		 				double length = obj.getLinkLength();
		 			
		 				
		 			System.out.println("\n"+activity 
						+"\nTravelTime :" + travelTimeString 
						+"\nAverageSpeed :" + v_mean
						+"\nLinkId : " + linkId_Veh_Id 
						+"\nPersonId :" + Person_id 
						+"\nLinklength : "+ length
						+"\nHbefaNr :"+ obj.getHbefa_Road_type());
		 				
		 				}catch(Exception e){}
		 			}
		    	}
		   }
		   	
}
		
		
	


