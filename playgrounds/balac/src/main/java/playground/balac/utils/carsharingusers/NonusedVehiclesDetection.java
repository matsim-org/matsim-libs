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

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;

public class NonusedVehiclesDetection {

	public static void main(String[] args) throws IOException {
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
	    int count = 0;
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
    		numberOfVehicles.put(arr[0], used);

	    	ids.put(arr[0], vehIDs);
	    	if (unused != 0)
	    		count+= unused;
	    	dataPoint.add(used);
	    	dataPoint.add(unused);
	    	dataPoint.add(0);
	    	vehicles.put(arr[0], dataPoint);
	    	
	    	s = reader.readLine();
	    	
	    }
	    
	    
	    
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
	    
	    double[] ratios = new double[vehicles.keySet().size()];
	    String[] connection = new String[vehicles.keySet().size()];
	    Object[] allIds =  vehicles.keySet().toArray();
	    
	    for(int j = 0; j < vehicles.keySet().size(); j++) {
		   
		   ratios[j] = vehicles.get((String)allIds[j]).get(2) / (vehicles.get((String)allIds[j]).get(1) + vehicles.get((String)allIds[j]).get(0));
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
	    while (count !=0)
	    
	    for (int k = 0; k < vehicles.keySet().size(); k++) {
	    	
	    	if (vehicles.get(connection[k]).get(1) == 0) {
	    		
	    		numberOfVehicles.put(connection[k], numberOfVehicles.get(connection[k]) + 1);
	    		count--;
	    	}
	    	
	    	
	    	if (count == 0)
	    		break;
	    	
	    }

		BufferedWriter output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/Stations_GreaterZurich_changed.txt")));

 reader = IOUtils.getBufferedReader(args[1]);
	    
	    s = reader.readLine();
	    s = reader.readLine();
	    int co = 0;
	    while(s != null) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	if (numberOfVehicles.get(arr[0]) != 0) {
	    	for (int h = 0; h < 6; h++  )
	    		output.write(arr[h] + "\t");
	    	
	    	output.write((Integer.toString(numberOfVehicles.get(arr[0]))));
	    	co += numberOfVehicles.get(arr[0]);
	    	output.newLine();
	    	}
	    	
	    	s = reader.readLine();
	    	
	    }
	    
	    output.flush();
	    output.close();
	    
	    
	    System.out.println(co);
	    
	    
		

	}

}
