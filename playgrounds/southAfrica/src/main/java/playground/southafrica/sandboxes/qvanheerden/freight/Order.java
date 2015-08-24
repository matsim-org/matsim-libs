package playground.southafrica.sandboxes.qvanheerden.freight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container class for orders used in {@link MyDemandGenerator}.
 * 
 * 
 * @author qvanheerden
 *
 */

public class Order {
	
	private int busMonth;
	private int busDay;
	private int calMonth;
	private int calDay;
	private int seqDay;
	private int dow;
	private String supplier;
	private String customer;
	private String group;
	private String product;
	private double mass;
	private double sale;
	private int daysSinceLast;
	private double lastMass;
	private double lastSale;
	private double distanceFromDepot;
	private List<String> listOfFields;
	
	//constructor with all fields, assuming all are available
	public Order(int busMonth, int busDay, String supplier, String customer, String group, String product, double mass, double sale){
		this.busMonth = busMonth;
		this.busDay = busDay;
		this.supplier = supplier;
		this.customer = customer;
		this.group = group;
		this.product = product;
		this.mass = mass;
		this.sale = sale;
	}

	//TODO could add methods to calculate some of the rest of the fields' values from these
	public Order(int busMonth, int busDay,int calMonth, int calDay, int seqDay,
					int dow, String supplier, String customer, String group, 
					String product, double mass, double sale, int daysSinceLastOrder, 
					double lastMass, double lastSale, double distance){
		this.busMonth = busMonth;
		this.busDay = busDay;
		this.calMonth = calMonth;
		this.calDay = calDay;
		this.seqDay = seqDay;
		this.dow = dow;
		this.supplier = supplier;
		this.customer = customer;
		this.group = group;
		this.product = product;
		this.mass = mass;
		this.sale = sale;
		this.daysSinceLast = daysSinceLastOrder;
		this.lastMass = lastMass;
		this.lastSale = lastSale;
		this.distanceFromDepot = distance;
	}
	
	public static String getHeaders(){
		return "busMonth,busDay,calMonth,calDay,seqDay,supplier,customer,group,product,mass,sale,daysSinceLast,lastMass,lastSale,distanceFromDepot";
	}
	
	//add method to get all values in a String array
	public List<String> getAllFields(){
		listOfFields = new ArrayList<String>();
		Collections.addAll(	listOfFields,
							String.valueOf(this.busMonth),
							String.valueOf(busDay),
							String.valueOf(calMonth),
							String.valueOf(calDay),
							String.valueOf(seqDay),
							String.valueOf(dow),
							supplier,
							customer,
							group,
							product,
							String.valueOf(mass),
							String.valueOf(sale),
							String.valueOf(daysSinceLast),
							String.valueOf(lastMass),
							String.valueOf(lastSale),
							String.valueOf(distanceFromDepot)
		);
		return listOfFields;
	}
	
	public void setDayOfWeek(int dow){
		this.dow = dow;
	}
	
	public int getDayOfWeek(){
		return this.dow;
	}
	
	public void setBusMonth(int busMonth){
		this.busMonth = busMonth;
	}
	
	public void setBusDay(int busDay){
		this.busDay = busDay;
	}
	
	public void setCalMonth(int calMonth){
		this.calMonth = calMonth;
	}
	
	public void setCalDay(int calDay){
		this.calDay = calDay;
	}
	
	public void setSeqDay(int seqDay){
		this.seqDay = seqDay;
	}
	
	public void setSupplier(String supplier){
		this.supplier = supplier;
	}
	
	public void setCustomer(String customer){
		this.customer = customer;
	}
	
	public void setGroup(String group){
		this.group = group;
	}
	
	public void setProduct(String product){
		this.product = product;
	}
	
	public void setMass(double mass){
		this.mass = mass;
	}
	
	public void setSale(double sale){
		this.sale = sale;
	}
	
	public void setDaysSinceLastOrder(int daysSinceLast){
		this.daysSinceLast = daysSinceLast;
	}
	
	public void setLastMass(double lastMass){
		this.lastMass = lastMass;
	}
	
	public void setLastSale(double lastSale){
		this.lastSale = lastSale;
	}

	public int getBusMonth(){
		return this.busMonth;
	}
	
	public int getBusDay(){
		return this.busDay;
	}
	
	public int getCalMonth(){
		return this.calMonth;
	}
	
	public int getCalDay(){
		return this.calDay;
	}
	
	public int getSeqDay(){
		return this.seqDay;
	}
	
	public String getSupplier(){
		return this.supplier;
	}
	
	public String getCustomer(){
		return this.customer;
	}
	
	public String getGroup(){
		return this.group;
	}
	
	public String getProduct(){
		return this.product;
	}
	
	public double getMass(){
		return this.mass;
	}
	
	public double getSale(){
		return this.sale;
	}
	
	public int getDaysSinceLastOrder(){
		return this.daysSinceLast;
	}
	
	public double getLastMass(){
		return this.lastMass;
	}
	
	public double getLastSale(){
		return this.lastSale;
	}

	public void setDistanceFromDepot(double distanceFromDepot){
		this.distanceFromDepot = distanceFromDepot;
	}

	public double getDistanceFromDepot(){
		return this.distanceFromDepot;
	}
	
}
