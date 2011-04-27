package playground.wrashid.sschieffer.DecentralizedSmartCharger;
/**
 * class that specifies on type of gas for a combustion engine; parameters to specify are:
 * <ul>
 * <li> joules per liter, e.g. Benzin 42,7–44,2 MJ/kg
 * <li> price per liter, e.g. 0.25 CHF
 * <li> emissions per liter //23,2kg/10l= xx/mass   1kg=1l
 * <li> name
 * </ul>
 * @author Stella
 *
 */
public class GasType {

	
	private double joulesPerLiter;
	private double pricePerLiter;
	private double emissionsPerLiter;
	
	private String name;
	
	public GasType(String name, double jPL, double pPL, double emissionsPerLiter){
		this.name=name;
		this.joulesPerLiter=jPL;
		this.pricePerLiter=pPL;
		this.emissionsPerLiter=emissionsPerLiter;
		
		
	}
	
	public String getName(){
		return name;
	}
	
	public double getJoulesPerLiter(){
		return joulesPerLiter;
	}
	
	
	public double getPricePerLiter(){
		return pricePerLiter;
	}
	
	public double getEmissionsPerLiter(){
		return emissionsPerLiter;
	}
	
}
