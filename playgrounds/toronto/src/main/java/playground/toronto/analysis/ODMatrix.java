package playground.toronto.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Basic class for holding large sparse 2-dimensional matrices (such as OD matrices). It
 * only stores data for mapped values, unmapped values are stored as a 'default value.'
 * 
 * Some basic cell-to-cell arithmetic functions are provided.
 * 
 * Could be better integrated with Matrices / Matrix in the future. Also implement
 * matrix algebra?
 * 
 * Also, I over-use HashMaps way too much. Any suggestions for other maps?
 * 
 * @author pkucirek
 *
 */
public class ODMatrix {

	private HashMap<String, Double> mappedValues;
	private final Set<String> allZones;
	private final double defaultValue;
	
	public ODMatrix(Set<String> set){
		this.allZones = set;
		this.mappedValues = new HashMap<String, Double>();
		this.defaultValue = 0.0;
	}
	public ODMatrix(Set<String> set, double defaultValue){
		this.allZones = set;
		this.mappedValues = new HashMap<String, Double>();
		this.defaultValue = defaultValue;
	}
	
	public void incrementODPair(String origin, String destination){
		if (!this.allZones.contains(origin)){
			throw new IllegalArgumentException("Origin zone \"" + origin + "\" does not exist!");
		}
		if (!this.allZones.contains(destination)){
			throw new IllegalArgumentException("Destination zone \"" + destination + "\" does not exist!");
		}
		String key = origin + "," + destination;
		Double val;
		if (this.mappedValues.containsKey(key)){
			val = this.mappedValues.get(key);
		}else{
			val = 1.0;
		}
		this.mappedValues.put(key, val);
	}
	public void incrementODPair(String origin, String destination, final double val){
		if (!this.allZones.contains(origin)){
			throw new IllegalArgumentException("Origin zone \"" + origin + "\" does not exist!");
		}
		if (!this.allZones.contains(destination)){
			throw new IllegalArgumentException("Destination zone \"" + destination + "\" does not exist!");
		}
		String key = origin + "," + destination;
		Double cell = 0.0;
		if (this.mappedValues.containsKey(key)){
			cell = this.mappedValues.get(key);
		}
		this.mappedValues.put(key, val + cell);
	}
	
	public void setODPair(String origin, String destination, Double val){
		if (!this.allZones.contains(origin)){
			throw new IllegalArgumentException("Origin zone \"" + origin + "\" does not exist!");
		}
		if (!this.allZones.contains(destination)){
			throw new IllegalArgumentException("Destination zone \"" + destination + "\" does not exist!");
		}
		String key = origin + "," + destination;
		this.mappedValues.put(key, val);
	}
	
	public double getODvalue(String origin, String destination){
		String key = origin + "," + destination;
		if (!this.mappedValues.containsKey(key)) return this.defaultValue;
		else return this.mappedValues.get(key);
	}
	private double getODvale(String key){
		if (!this.mappedValues.containsKey(key)) return this.defaultValue;
		else return this.mappedValues.get(key);
	}
	
	public ODMatrix divideBy(ODMatrix divisor){
		if (!divisor.allZones.containsAll(allZones))
			throw new IllegalArgumentException("Can only compare similar zone systems!");
		
		double newDefaultValue = (divisor.defaultValue == 0.0) ? 
				Double.NaN : this.defaultValue / divisor.defaultValue;
		ODMatrix result = new ODMatrix(allZones, newDefaultValue);
		
		Set<String> keySet = new HashSet<String>();
		keySet.addAll( divisor.mappedValues.keySet());
		keySet.addAll(this.mappedValues.keySet());
		for (String key : keySet){
			double denominator = divisor.getODvale(key);
			double res;
			if (denominator == 0.0) res = Double.NaN;
			else res = this.getODvale(key) / denominator;
			
			result.mappedValues.put(key, res);
		}
		
		return result;
	}
	
	public ODMatrix addTo(ODMatrix matrix){
		if (!matrix.allZones.containsAll(allZones))
			throw new IllegalArgumentException("Can only compare similar zone systems!");
		double newDefaultValue = this.defaultValue + matrix.defaultValue;
		ODMatrix result = new ODMatrix(allZones, newDefaultValue);
		
		Set<String> keySet = new HashSet<String>();
		keySet.addAll(matrix.mappedValues.keySet());
		keySet.addAll(this.mappedValues.keySet());
		for (String key : keySet){
			double res = this.getODvale(key) + matrix.getODvale(key);
			result.mappedValues.put(key, res);
		}
		
		return result;
	}
	
	public void exportAs311File(String filename, int i, String name6char) throws IOException{
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write("c Matrix exported from MATSim");
		writer.newLine(); writer.write("c Exported at " + dateFormat.format(new Date()));
		writer.newLine(); writer.write("t matrices");
		writer.newLine(); writer.write("a mf" + i + " " + name6char + " default=" + this.defaultValue);
		
		for(Entry<String, Double> e : this.mappedValues.entrySet()){
			int origin = Integer.parseInt(e.getKey().split(",")[0]);
			int destination = Integer.parseInt(e.getKey().split(",")[1]);
			double val = Math.round(e.getValue() * 100.0) / 100.0;
			
			writer.newLine();
			writer.write(" " + origin + " " + destination + ":" + val);
		}
		writer.close();
		
	}
	
}
