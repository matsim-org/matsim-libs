package playground.dhosse.gap.scenario.population;

public class Municipality {
	public int n;
	public int nStudents;
	public int nAdults;
	public int nPensioners;

	public Municipality(int nStudents, int nAdults, int nPensioners){
		
		this.nStudents = nStudents;
		this.nAdults = nAdults;
		this.nPensioners = nPensioners;
		
		this.n = nStudents + nAdults + nPensioners;
		
	}

	public int getN() {
		return n;
	}

	public int getnStudents() {
		return nStudents;
	}

	public int getnAdults() {
		return nAdults;
	}

	public int getnPensioners() {
		return nPensioners;
	}
	
}