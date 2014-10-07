package playground.mmoyo.analysis.counts.reader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**parses a output text file containing counts comparisons*/
public class CountsReader {

	final String SEPARATOR = "\\t";
	final String NULL_STRING ="";
	final String STOP_ID_STRING_0 = "StopId :";
	final String HEAD_STRING_0 = "hour";
	final String ZERO = "0.0";
	
	String countsTextFile;
	Map <Id, Map <String,double[] >> count = new TreeMap <Id, Map<String, double[]>>();  //TODO : make Map <Id, Map <double[24][2] >> 
	
	public CountsReader(final String countsTextFile){
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
			Id<Link> id= Id.create(values[1], Link.class);
			while(row != null) {
				row = bufferedReader.readLine();
				if(row != null && row != NULL_STRING) {				
					values = row.split(SEPARATOR); 
					if (values[0].equals(STOP_ID_STRING_0)){     
						id = Id.create(values[1], Link.class);
					}else if (values[0].equals(HEAD_STRING_0)){    
					  //it does nothing, correct this condition	
					}else{
						if (!count.containsKey(id)){
							count.put(id, new TreeMap< String, double[]>() );   
						}
						count.get(id).put(values[0], new double[] {Double.parseDouble(values[1]), Double.parseDouble(values[2]), Double.parseDouble(values[3])} );
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
	
	public double[]getSimulatedValues(final Id stopId) {
		return this.getCountValues(stopId, 0);
	}

	public double[]getSimulatedScaled(final Id stopId) {
		return this.getCountValues(stopId, 1);
	}

	public double[]getRealValues(final Id stopId) {
		return this.getCountValues(stopId, 2);
	}

	private double[]getCountValues(final Id stopId, final int col){ 
		double[] valueArray = new double[24];      
		for (byte i= 0; i<24 ; i++)   {  
			String hour = String.valueOf(i+1);
			if (count.keySet().contains(stopId)){
				double[] value = count.get(stopId).get(hour);
				if (value == null){
					valueArray[i] = 0.0;
				}else{
 					valueArray[i] = value[col] ;  //0 = simulated; 1= simulatedEscaled ;  2=realValues    
				}
			}else{
				valueArray = null;
			}
		}
		return valueArray;
	}
	
	/**
	 * @return returns a id set of stops listed in the text file 
	 */
	public Set<Id> getStopsIds(){
		return count.keySet();
	}
	
	public static void main(String[] args) {
		String countComparisonFile;
		if(args.length == 1){
			countComparisonFile = args[0];
		}else{
			countComparisonFile ="../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.simBseCountCompareOccupancy.txt";
		}
			
		CountsReader countReader = new CountsReader(countComparisonFile);

		String tab= "\t";
		for (Id id : countReader.getStopsIds()){
			System.out.println(id);
			for (int h=0; h<24; h++ ){
				double sim  = countReader.getSimulatedValues(id)[h];
				double simEscaled = countReader.getSimulatedScaled(id)[h];
				double real = countReader.getRealValues(id)[h];
				System.out.println((h+1) + tab + sim + tab + simEscaled + tab + real);
			}
		}
		
	}
	
}
