package sandbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kid.ScheduledVehicles;

import org.apache.commons.math.stat.Frequency;
import org.apache.log4j.Logger;

import playground.mzilske.freight.CarrierImpl;
import sandbox.Classifier.FrequencyClass;
import sandbox.SandBoxTrafficGenerator.Company;
import stats.EmpiricalWalker;

public class VehicleFleetBuilder {
	
	public Map<String, EmpiricalWalker> getLkwWalkers() {
		return lkwWalkers;
	}

	public Map<String, EmpiricalWalker> getPkwWalkers() {
		return pkwWalkers;
	}

	static class CompanyClass {
		
		public static String MICRO = "micro";
		
		public static String SMALL = "small";
		
		public static String MEDIUM = "medium";
		
		public static String BIG = "big";
		
		public static String HUGE = "huge";
		
		private String companySizeType;
		
		private FrequencyClass frequencyClass;

		public CompanyClass(String companySize, FrequencyClass frequencyClass) {
			super();
			this.companySizeType = companySize;
			this.frequencyClass = frequencyClass;
		}

		public String getCompanySizeType() {
			return companySizeType;
		}

		public FrequencyClass getFrequencyClass() {
			return frequencyClass;
		}
	}
	
	private static Logger logger = Logger.getLogger(VehicleFleetBuilder.class);
	
	private ScheduledVehicles vehicles;
	
	private Map<String, EmpiricalWalker> lkwWalkers = new HashMap<String, EmpiricalWalker>();
	
	private Map<String, EmpiricalWalker> pkwWalkers = new HashMap<String, EmpiricalWalker>();
	
	/**
	 * Wie berichtet ein Unternehmen?
	 * Möglichkeit 1: nur ein Fahrzeug
	 * Möglichkeit 2: sämtliche Fahrzeuge aus Fuhrpark an einem Tag
	 * 
	 * 
	 * vorgefilterte vehicles, d.h. z.B. nach Wirtschaftszweig gefiltert 
	 * 
	 * @param kidVehicles
	 */
	 
	private List<CompanyClass> companyClasses;
	
	private VehicleStats vehicleStats;
	
	public VehicleFleetBuilder(ScheduledVehicles vehicles, List<CompanyClass> companyClasses) {
		super();
		this.vehicles = vehicles;
		this.companyClasses = companyClasses;
		makeVehicleStats();
		buildEmpiricalWalkers();
	}

	private void makeVehicleStats() {
		logger.info("build vehicle statistics");
		vehicleStats = new VehicleStats(vehicles);
	}

	private void buildEmpiricalWalkers() {
		logger.info("build empirical walkers");
		buildPkwWalkers();
		buildLkwWalkers();
	}

	private void buildLkwWalkers() {
		for(CompanyClass c : companyClasses){
			Frequency conditionalFrequency = Classifier.makeConditionalFrequency(c.getFrequencyClass(), 
					vehicleStats.getEmployees(), vehicleStats.getLkw());
			lkwWalkers.put(c.getCompanySizeType(), new EmpiricalWalker(conditionalFrequency));
		}
		
	}

	private void buildPkwWalkers() {
		for(CompanyClass c : companyClasses){
			Frequency conditionalFrequency = Classifier.makeConditionalFrequency(c.getFrequencyClass(), 
					vehicleStats.getEmployees(), vehicleStats.getPkw());
			pkwWalkers.put(c.getCompanySizeType(), new EmpiricalWalker(conditionalFrequency));
		}
		
	}

	public void buildVehicleFleet(Company company, CarrierImpl carrier){
		company.getnOfEmployees();
		
	}
	
	public double getNoPkw(Company company){
		CompanyClass companyClass = getCompanyClass(company.getnOfEmployees());
		return (Double) pkwWalkers.get(companyClass.getCompanySizeType()).nextValue();
	}
	
	public double getNoLkw(Company company){
		CompanyClass companyClass = getCompanyClass(company.getnOfEmployees());
		return (Double) lkwWalkers.get(companyClass.getCompanySizeType()).nextValue();
	}

	private CompanyClass getCompanyClass(int getnOfEmployees) {
		for(CompanyClass companyClass : companyClasses){
			if(companyClass.getFrequencyClass().hasValue(getnOfEmployees)){
				return companyClass;
			}
		}
		return null;
	}

}
