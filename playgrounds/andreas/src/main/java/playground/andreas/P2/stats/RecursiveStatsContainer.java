package playground.andreas.P2.stats;

import org.apache.log4j.Logger;

/**
 * Collects average number of cooperatives, passengers and vehicles and its variance in a recursive manner
 *  
 * @author aneumann
 *
 */
public class RecursiveStatsContainer {
	
	private static final Logger log = Logger.getLogger(RecursiveStatsContainer.class);
	static final String toStringHeader = "# mean Coops; std dev Coops; mean Pax; std dev Pax; mean Veh, std dev Veh"; 

	private double numberOfEntries = Double.NaN;
	
	private double arithmeticMeanCoops;
	private double tempVarCoops;
	private double arithmeticMeanPax;
	private double tempVarPax;
	private double arithmeticMeanVeh;
	private double tempVarVeh;
	
	public void handleNewEntry(double coops, double pax, double veh){
		// new entries n + 1
		double meanCoops_n_1;
		double tempVarCoops_n_1;
		double meanPax_n_1;
		double tempVarPax_n_1;
		double meanVeh_n_1;
		double tempVarVeh_n_1;

		if(Double.isNaN(this.numberOfEntries)){
			// initialize
			this.numberOfEntries = 0;
			this.arithmeticMeanCoops = 0;
			this.tempVarCoops = 0;
			this.arithmeticMeanPax = 0;
			this.tempVarPax = 0;;
			this.arithmeticMeanVeh = 0;
			this.tempVarVeh = 0;;
		}

		// calculate the exact mean and variance

		// calculate new mean
		meanCoops_n_1 =  (this.numberOfEntries * this.arithmeticMeanCoops + coops) / (this.numberOfEntries + 1);
		meanPax_n_1 =  (this.numberOfEntries * this.arithmeticMeanPax + pax) / (this.numberOfEntries + 1);
		meanVeh_n_1 =  (this.numberOfEntries * this.arithmeticMeanVeh + veh) / (this.numberOfEntries + 1);

		if (this.numberOfEntries == 0) {
			tempVarCoops_n_1 = 0;
			tempVarPax_n_1 = 0;
			tempVarVeh_n_1 = 0;
		} else {
			tempVarCoops_n_1 = this.tempVarCoops + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanCoops_n_1 - coops) * (meanCoops_n_1 - coops);
			tempVarPax_n_1 = this.tempVarPax + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanPax_n_1 - pax) * (meanPax_n_1 - pax);
			tempVarVeh_n_1 = this.tempVarVeh + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanVeh_n_1 - veh) * (meanVeh_n_1 - veh);
		}
		
		this.numberOfEntries++;

		// store em away
		this.arithmeticMeanCoops = meanCoops_n_1;
		this.tempVarCoops = tempVarCoops_n_1;
		this.arithmeticMeanPax = meanPax_n_1;
		this.tempVarPax = tempVarPax_n_1;
		this.arithmeticMeanVeh = meanVeh_n_1;
		this.tempVarVeh = tempVarVeh_n_1;
	}

	public double getArithmeticMeanCoops() {
		return this.arithmeticMeanCoops;
	}

	public double getStdDevCoop() {

		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarCoops);
		}
		
		return Double.NaN;
	}

	public double getArithmeticMeanPax() {
		return this.arithmeticMeanPax;
	}

	public double getStdDevPax() {

		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarPax);
		}
		
		return Double.NaN;
	}

	public double getArithmeticMeanVeh() {
		return this.arithmeticMeanVeh;
	}

	public double getStdDevVeh() {

		if (this.numberOfEntries > 1){
			return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVarVeh);
		}
		
		return Double.NaN;
	}

	@Override
	public String toString() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.getArithmeticMeanCoops()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevCoop()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanPax()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevPax()); strBuffer.append("; ");
		strBuffer.append(this.getArithmeticMeanVeh()); strBuffer.append("; ");
		strBuffer.append(this.getStdDevVeh()); strBuffer.append("; ");
		return strBuffer.toString();
	}	
}