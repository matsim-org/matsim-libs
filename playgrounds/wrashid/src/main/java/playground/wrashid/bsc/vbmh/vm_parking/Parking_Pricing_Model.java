package playground.wrashid.bsc.vbmh.vm_parking;

/**
 * Parameters for the pricing models. 
 * !! Preis im Model berechnen um Flexibler zu sein / Model eher als Interface?
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class Parking_Pricing_Model {
	public int id;
	private double price_per_minute_ev;
	private double price_per_minute_nev;
	private double price_of_first_minute_ev;
	private double price_of_first_minute_nev;
	private double max_time_ev;
	private double max_time_nev;
	
	
	
	public double getPrice_per_minute_ev() {
		return price_per_minute_ev;
	}
	public void setPrice_per_minute_ev(double price_per_minute_ev) {
		this.price_per_minute_ev = price_per_minute_ev;
	}
	public double getPrice_per_minute_nev() {
		return price_per_minute_nev;
	}
	public void setPrice_per_minute_nev(double price_per_minute_nev) {
		this.price_per_minute_nev = price_per_minute_nev;
	}
	public double getPrice_of_first_minute_ev() {
		return price_of_first_minute_ev;
	}
	public void setPrice_of_first_minute_ev(double price_of_first_minute_ev) {
		this.price_of_first_minute_ev = price_of_first_minute_ev;
	}
	public double getPrice_of_first_minute_nev() {
		return price_of_first_minute_nev;
	}
	public void setPrice_of_first_minute_nev(double price_of_first_minute_nev) {
		this.price_of_first_minute_nev = price_of_first_minute_nev;
	}
	public double getMax_time_nev() {
		return max_time_nev;
	}
	public void setMax_time_nev(double max_time_nev) {
		this.max_time_nev = max_time_nev;
	}
	public double getMax_time_ev() {
		return max_time_ev;
	}
	public void setMax_time_ev(double max_time_ev) {
		this.max_time_ev = max_time_ev;
	}
}
