/**
 * 
 */
package sandbox;

import java.util.ArrayList;
import java.util.List;

import kid.KiDSchema;
import kid.ScheduledVehicle;
import kid.ScheduledVehicles;
import kid.Vehicle;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;

/**
 * @author stefan
 *
 */
public class VehicleStats {
	
	private static Logger log = Logger.getLogger(VehicleStats.class);
	
	private ScheduledVehicles vehicles;

	private double[] pkw;
	
	private double[] lkw;
	
	private double[] employees;
	
	public double[] getPkw() {
		return pkw;
	}

	public double[] getLkw() {
		return lkw;
	}

	public VehicleStats(ScheduledVehicles vehicles) {
		super();
		this.vehicles = vehicles;
		generateFrequencies();
	}
	
	private void generateFrequencies(){
		List<Double> pkw = new ArrayList<Double>();
		List<Double> lkw = new ArrayList<Double>();
		List<Double> employees = new ArrayList<Double>();
		for(ScheduledVehicle sVehicle : vehicles.getScheduledVehicles().values()){
			Vehicle vehicle = sVehicle.getVehicle();
			double nOfPkw = getPkw(vehicle);
			pkw.add(nOfPkw);
			double nOfLkw = getLkw(vehicle);
			lkw.add(nOfLkw);
			double nOfEmployees = getNuOfEmployees(vehicle);
			employees.add(nOfEmployees);
		}
		this.pkw = getArray(pkw);
		this.lkw = getArray(lkw);
		this.employees = getArray(employees);
		log.debug("datasets -> [#pkws="+pkw.size()+"][#lkws="+lkw.size()+"][#employees=" + employees.size()+"]");
	}
	
	private double getNuOfEmployees(Vehicle vehicle) {
		String empl = vehicle.getAttributes().get(KiDSchema.COMPANY_EMPLOYEES);
		return Integer.parseInt(empl);
	}

	public double computeVehicleCorrelation(){
		PearsonsCorrelation pearson = new PearsonsCorrelation();
		return pearson.correlation(pkw, lkw);
	}
	
	public double computeCorrelation(double[] val1, double[] val2){
		PearsonsCorrelation pearson = new PearsonsCorrelation();
		return pearson.correlation(val1,val2);
	}
	
	public double[] getEmployees() {
		return employees;
	}

	public void plot(String filename, String title, double[] values){
		BarChart barChart = getBarChart(title, values);
		barChart.saveAsPng(filename, 800, 600);
	}
	
	public void plotLkws(String filename){
		BarChart barChart = getBarChart("lkw", lkw);
		barChart.saveAsPng("output/lkw.png", 800, 600);
	}
	
	public void plotPkws(String filename){
		BarChart barChart = getBarChart("pkw", pkw);
		barChart.saveAsPng("output/pkw.png", 800, 600);
	}

	private BarChart getBarChart(String title, double[] vehicles) {
		BarChart barChart = new BarChart(title, "nOfVehicles", "frequency");
		barChart.addSeries(title, vehicles);
		return barChart;
	}

	private double[] getArray(List<Double> value) {
		double[] valArr = new double[value.size()];
		for(int i=0;i<value.size();i++){
			valArr[i] = value.get(i);
		}
		return valArr;
	}

	private double getPkw(Vehicle vehicle) {
		String pkw = vehicle.getAttributes().get(KiDSchema.COMPANY_FUHRPARK_PKW);
		return Integer.parseInt(pkw);
	}

	private int getLkw(Vehicle vehicle) {
		String lkw = vehicle.getAttributes().get(KiDSchema.COMPANY_FUHRPARK_LKW);
		return Integer.parseInt(lkw);
	}

}
