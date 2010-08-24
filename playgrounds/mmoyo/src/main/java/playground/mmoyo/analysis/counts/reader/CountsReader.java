package playground.mmoyo.analysis.counts.reader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**parses a output text file containing counts comparisons*/
public class CountsReader {

	final String SEPARATOR = "\\t";
	final String NULL_STRING ="";
	final String STOP_ID_STRING_0 = "StopId :";
	final String HEAD_STRING_0 = "hour";
	final String ZERO = "0.0";
	
	String countsTextFile;
	Map <Id, Map <String,double[] >> count = new TreeMap <Id, Map<String, double[]>>();  //TODO : make Map <Id, Map <double[24][2] >> 
	
	public CountsReader(String countsTextFile){
		this.countsTextFile = countsTextFile;
		readValues();
	}
	
	private void readValues(){
		try {
			FileReader fileReader =  new FileReader(this.countsTextFile);
			//->:correct this : reads first row 
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String row = bufferedReader.readLine();		//TODO : include the first row inside the iteration 
			String[] values = row.split(SEPARATOR);				
			Id id= new IdImpl (values[1]);
			while(row != null) {
				row = bufferedReader.readLine();
				if(row != null && row != NULL_STRING) {				
					values = row.split(SEPARATOR); 
					if (values[0].equals(STOP_ID_STRING_0)){     
						id = new IdImpl (values[1]);
					}else if (values[0].equals(HEAD_STRING_0)){    
					  //it does nothing, correct this condition	
					}else{
						if (!count.containsKey(id)){
							count.put(id, new TreeMap< String, double[]>() );   
						}
						count.get(id).put(values[0], new double[] {Double.parseDouble(values[3]), Double.parseDouble(values[2])} );
					}
				}
			}
			bufferedReader.close();
			fileReader.close();
		}catch(Exception e) { 
	    	System.out.println(e.toString());
	    	e.printStackTrace();
	    }
	}
	
	/**
	 * @param stopId
	 * @param simValues true returns the real count values , false return the simulation values
	 */
	public double[]getRoutValues(final Id stopId, final boolean simValues){ 
		double[] valueArray = new double[24];      
		for (byte i= 0; i<24 ; i++)   {  
			String hour = String.valueOf(i+1);
			double[] value = count.get(stopId).get(hour);
			if (value == null){
				valueArray[i] = 0.0;
			}else{
				int index= (simValues)?1:0; //converts from boolean into integer: //0 = countValue   1 = simValue(scaled)  
				valueArray[i] = value[index] ;   
			}
		}
		return valueArray;
	}
	
	
	/**
	 * returns the volumes per hour
	 * @param simValues true returns the real count values , false return the simulation values
	 */
	public double[]getVolumeValues(final boolean simValues){ 
		double[] volume = new double[24];      
		for (Map <String,double[]> values : count.values()){
			for (double[] values2 : values.values()){
				for (byte i= 0; i<24 ; i++)   {  
					volume[i] = values2[simValues?1:0];    // simValues?1:0   converts from boolean into integer: //0 = countValue   1 = simValue(scaled)
				}
			}
		}
		return volume;
	}
	
	
	/**
	 * @return returns a id set of stops listed in the text file 
	 */
	public Set<Id> getStopsIds(){
		return count.keySet();
	}
	
}
