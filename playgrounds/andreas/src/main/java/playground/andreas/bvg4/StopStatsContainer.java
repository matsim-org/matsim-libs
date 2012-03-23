package playground.andreas.bvg4;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * Collects stats for one transit route stop
 *  
 * @author aneumann
 *
 */
public class StopStatsContainer {
	
	private static final Logger log = Logger.getLogger(StopStatsContainer.class);
	static final String toStringHeader = "# StopId; n Arr; mean Arr; std dev Arr; n Dep; mean Dep; std dev Dep"; 

	private Id stopId;

	private double numberOfEntriesArrvial = Double.NaN;
	private double arithmeticMeanArrvial;
	private double tempVarArrival;
	
	private double numberOfEntriesDeparture = Double.NaN;
	private double arithmeticMeanDeparture;
	private double tempVarDeparture;

	public StopStatsContainer(Id stopId) {
		this.stopId = stopId;
	}
	
	public void addArrival(double offset){
		// new entries n + 1
		double mean_n_1;
		double tempVarArrival_n_1;
		
		if(Double.isNaN(this.numberOfEntriesArrvial)){
			// initialize
			this.numberOfEntriesArrvial = 0;
			this.arithmeticMeanArrvial = 0;
			this.tempVarArrival = 0;;
		}
		
		// calculate new mean
		mean_n_1 =  (this.numberOfEntriesArrvial * this.arithmeticMeanArrvial + offset) / (this.numberOfEntriesArrvial + 1);
		this.numberOfEntriesArrvial++;
		
		if (this.numberOfEntriesArrvial == 1) {
			tempVarArrival_n_1 = 0;
		} else {
			tempVarArrival_n_1 = this.tempVarArrival + this.numberOfEntriesArrvial / (this.numberOfEntriesArrvial - 1) * (mean_n_1 - offset) * (mean_n_1 - offset);
		}
		
		// store em away
		this.arithmeticMeanArrvial = mean_n_1;
		this.tempVarArrival = tempVarArrival_n_1;
	}
	
	public void addDeparture(double offset){
		// new entries n + 1
		double mean_n_1;
		double tempVarDeparture_n_1;
		
		if(Double.isNaN(this.numberOfEntriesDeparture)){
			// initialize
			this.numberOfEntriesDeparture = 0;
			this.arithmeticMeanDeparture = 0;
			this.tempVarDeparture = 0;;
		}
		
		// calculate new mean
		mean_n_1 =  (this.numberOfEntriesDeparture * this.arithmeticMeanDeparture + offset) / (this.numberOfEntriesDeparture + 1);
		this.numberOfEntriesDeparture++;
		
		if (this.numberOfEntriesDeparture == 1) {
			tempVarDeparture_n_1 = 0;
		} else {
			tempVarDeparture_n_1 = this.tempVarDeparture + this.numberOfEntriesDeparture / (this.numberOfEntriesDeparture - 1) * (mean_n_1 - offset) * (mean_n_1 - offset);
		}
		
		// store em away
		this.arithmeticMeanDeparture = mean_n_1;
		this.tempVarDeparture = tempVarDeparture_n_1;
	}
	
	public double getNumberOfEntriesArrival(){
		return this.numberOfEntriesArrvial;
	}
	
	public double getArithmeticMeanArrival(){
		return this.arithmeticMeanArrvial;
	}
	
	public double getStandardDeviationArrival(){
		
		if (this.numberOfEntriesArrvial > 1){
			return Math.sqrt(1.0/(this.numberOfEntriesArrvial - 1.0) * this.tempVarArrival);
		}
		
		return Double.NaN;
	}
	
	public double getNumberOfEntriesDeparture(){
		return this.numberOfEntriesDeparture;
	}
	
	public double getArithmeticMeanDeparture(){
		return this.arithmeticMeanDeparture;
	}
	
	public double getStandardDeviationDeparture(){
		
		if (this.numberOfEntriesDeparture > 1){
			return Math.sqrt(1.0/(this.numberOfEntriesDeparture - 1.0) * this.tempVarDeparture);
		}
		
		return Double.NaN;
	}
	
	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.stopId); strBuffer.append("; ");
		strBuffer.append(this.getNumberOfEntriesArrival()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanArrival()); strBuffer.append("; ");
		strBuffer.append(this.getStandardDeviationArrival()); strBuffer.append("; ");
		strBuffer.append(this.getNumberOfEntriesDeparture()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanDeparture()); strBuffer.append("; ");
		strBuffer.append(this.getStandardDeviationDeparture());
		return strBuffer.toString();
	}	
}