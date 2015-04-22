package playground.balac.utils.carsharingusers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class GeneratingFleetFromDemand {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		Set<String> usedCars = new HashSet<String>();
		Set<String> unusedCars = new HashSet<String>();
		Map<String, ArrayList<Integer>> vehicles = new HashMap<String, ArrayList<Integer>>();
		
		Map<String, ArrayList<String>> ids = new HashMap<String, ArrayList<String>>();
		Map<String, String> idsMap = new HashMap<String, String>();
		Map<String, Integer> numberOfVehicles = new HashMap<String, Integer>();

		/*ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[2]);*/
		
		//TODO: create a HashMap with Link and number of cars that was not used
		
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);
	    String s = reader.readLine();
	    s = reader.readLine();
	    
	    while (s != null) {
	    	
	    	String[] arr = s.split("\\s");
			usedCars.add(arr[7]);

			s = reader.readLine();
	    }
	    
	    
	    reader = IOUtils.getBufferedReader(args[1]);
	    
	    s = reader.readLine();
	    s = reader.readLine();
	    int i = 1;
	    while(s != null) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	//CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
	    	//Link l = ((NetworkImpl)scenario.getNetwork()).getNearestLinkExactly(coordStart);			    	
			ArrayList<String> vehIDs = new ArrayList<String>();
	    	ArrayList<Integer> dataPoint = new ArrayList<Integer>();
	    	int used = 0;
	    	int unused = 0;

	    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
	    		String vehId = "TW_"+ Integer.toString(i);
	    		vehIDs.add(vehId);
	    		idsMap.put(vehId, arr[0]);

	    		if (!usedCars.contains(vehId)) {
	    			unusedCars.add(vehId);
	    			unused++;
	    		}
	    		else
	    			used++;
	    		i++;
	    	}
    		//numberOfVehicles.put(arr[0], used);

	    	ids.put(arr[0], vehIDs);
	    	
	    	dataPoint.add(used);
	    	dataPoint.add(unused);
	    	dataPoint.add(0);
	    	vehicles.put(arr[0], dataPoint);
	    	
	    	s = reader.readLine();
	    	
	    }
	    reader.close();
	    reader = IOUtils.getBufferedReader(args[0]);
	    s = reader.readLine();
	    s = reader.readLine();
	    
	    while (s != null) {
	    	
	    	String[] arr = s.split("\\s");
	    	
	    	String v = idsMap.get(arr[7]);
	    	ArrayList<Integer> al = vehicles.get(v);
	    	
	    	int x = al.get(2);
	    	
	    	x++;
	    	al.remove(2);
	    	al.add(x);
	    	
			usedCars.add(arr[7]);

			s = reader.readLine();
	    }
	    reader.close();
	    double[] ratios = new double[vehicles.keySet().size()];
	    String[] connection = new String[vehicles.keySet().size()];
	    Object[] allIds =  vehicles.keySet().toArray();
	    
	    for(int j = 0; j < vehicles.keySet().size(); j++) {
		   
		   ratios[j] = (double)vehicles.get((String)allIds[j]).get(2) / (double)(vehicles.get((String)allIds[j]).get(1) + (double)vehicles.get((String)allIds[j]).get(0));
		   connection[j] = (String)allIds[j];
	   }
	    
	    
	    for (int k = 0; k < vehicles.keySet().size() - 1; k++)
		    for (int l = k + 1; l < vehicles.keySet().size(); l++) {
		    	
		    	if (ratios[k] < ratios[l]) {
		    		
		    		double a =  ratios[k];
		    		String b = connection[k];
		    		
		    		ratios[k] = ratios[l];
		    		ratios[l] = a;
		    		connection[k] = connection[l];
		    		connection[l] = b;
		    	}
		    }
	    

		BufferedWriter output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/Stations_GreaterZurich_from_demand_run34.txt")));

		reader = IOUtils.getBufferedReader(args[1]);
	    
	    s = reader.readLine();
	    s = reader.readLine();
	    int co = 0;
	    
	    
	    for (int k = 0; k < ratios.length; k++) {
	    	if (co + vehicles.get(connection[k]).get(0) < 911)
	    	numberOfVehicles.put(connection[k], vehicles.get(connection[k]).get(0));
	    	else {
		    	numberOfVehicles.put(connection[k], 911 - co);
		    	co += 911 - co;
		    	break;
	    	}
	    	co += vehicles.get(connection[k]).get(0);
	    	
	    }
	    while(s != null ) {
	    	
	    	String[] arr = s.split("\t", -1);	    	
	    	
	    	for (int h = 0; h < 6; h++  )
	    		output.write(arr[h] + "\t");
	    	
	    	if (numberOfVehicles.containsKey(arr[0])) 
	    		output.write(Integer.toString(numberOfVehicles.get(arr[0])));
	    	else
	    		output.write(Integer.toString(0));
	    	output.newLine();
	    	
	    	
	    	s = reader.readLine();
	    	
	    }
	    reader.close();
	    output.flush();
	    output.close();
	    
	    reader = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Stations_GreaterZurich_from_demand_run34.txt");
	    output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/Stations_GreaterZurich_from_demand_run34_v2.txt")));
	    s = reader.readLine();
	    while(s != null ) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	if (!arr[6].equals("0")){
	    	
		    	for (int h = 0; h < 6; h++  )
		    		output.write(arr[h] + "\t");
		    	
		    	
		    	output.write(arr[6]);
		    	
		    	output.newLine();
	    	}
	    	
	    	s = reader.readLine();
	    	
	    }
	    reader.close();
	    output.flush();
	    output.close();
	    
	    
	    
	    System.out.println(co);

	}

}
