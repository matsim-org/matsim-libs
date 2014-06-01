package playground.vbmh.vmParking;

/**
 * Implementation of the static pricing models which are given in the XML input file; Calculates the costs for a given
 * vehicle type and a given duration.
 * 
 * 
 * !! Preis im Model berechnen um Flexibler zu sein / Model eher als Interface?
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */


public class ParkingPricingModel {
	public int id;
	private double pricePerMinuteEV;
	private double pricePerMinuteNEV;
	private double priceOfFirstMinuteEV;
	private double priceOfFirstMinuteNEV;
	private double maxTimeEV;
	private double maxTimeNEV;
	private Boolean evExclusive; //=false; 
	
	
	public double calculateParkingPrice(double duration, boolean ev){
		duration = duration/3600; //von Sekunden auf Stunden
		double price = 0;
		//System.out.println(evExclusive);
		if (ev){
			
			price = this.getPriceOfFirstMinuteEV() + duration * this.getPricePerMinuteEV();
		} else {
			if(evExclusive != null){ //Sollte nicht mehr passieren >> EVExclusives sollten bei NEV garnicht merh in die Liste kommen
				if(evExclusive){
					System.out.println("EV exclusive! (should not happen!)");
					return -1; // NEV is not allowed to park on this spot
				}
			}
			//System.out.println(model.getPricePerMinuteNEV());
			price = this.getPriceOfFirstMinuteNEV() + duration * this.getPricePerMinuteNEV();
		}
		
		return price;
	}
	
	public double calculateParkingPrice(double duration, boolean ev, ParkingSpot spot){
		// Nur zum ueberschreiben falls man Informationen vom Spot mit in den Preis 
		// einbeziehen moechte
		return this.calculateParkingPrice(duration, ev);
	}
	public Boolean checkEvExc(){
		if (evExclusive!= null){
			if(evExclusive){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	
	public double getPricePerMinuteEV() {
		return pricePerMinuteEV;
	}
	public void setPricePerMinuteEV(double price_per_minute_ev) {
		this.pricePerMinuteEV = price_per_minute_ev;
	}
	public double getPricePerMinuteNEV() {
		return pricePerMinuteNEV;
	}
	public void setPricePerMinuteNEV(double price_per_minute_nev) {
		this.pricePerMinuteNEV = price_per_minute_nev;
	}
	public double getPriceOfFirstMinuteEV() {
		return priceOfFirstMinuteEV;
	}
	public void setPriceOfFirstMinuteEV(double price_of_first_minute_ev) {
		this.priceOfFirstMinuteEV = price_of_first_minute_ev;
	}
	public double getPriceOfFirstMinuteNEV() {
		return priceOfFirstMinuteNEV;
	}
	public void setPriceOfFirstMinuteNEV(double price_of_first_minute_nev) {
		this.priceOfFirstMinuteNEV = price_of_first_minute_nev;
	}
	public double getMaxTimeNEV() {
		return maxTimeNEV;
	}
	public void setMaxTimeNEV(double max_time_nev) {
		this.maxTimeNEV = max_time_nev;
	}
	public double getMaxTimeEV() {
		return maxTimeEV;
	}
	public void setMaxTimeEV(double max_time_ev) {
		this.maxTimeEV = max_time_ev;
	}

	public Boolean getEvExclusive() {
		return evExclusive;
	}

	public void setEvExclusive(Boolean evExclusive) {
		this.evExclusive = evExclusive;
	}
}
