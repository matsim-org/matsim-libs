package playground.dziemke.potsdam.analysis.aggregated;

public class PairPtCar {

	private int arrivals_pt;
	private int arrivals_car;
	
	public PairPtCar(int arrival_pt, int arrivals_car){
		this.arrivals_pt = arrival_pt;
		this.arrivals_car = arrivals_car;
	}

	public int getArrivals_pt() {
		return arrivals_pt;
	}

	public void setArrivals_pt(int arrivals_pt) {
		this.arrivals_pt = arrivals_pt;
	}

	public int getArrivals_car() {
		return arrivals_car;
	}

	public void setArrivals_car(int arrivals_car) {
		this.arrivals_car = arrivals_car;
	}

	@Override
	public String toString() {
		return "ModalSplit [arrivals_pt=" + arrivals_pt + ", arrivals_car="
				+ arrivals_car + "]";
	}
}