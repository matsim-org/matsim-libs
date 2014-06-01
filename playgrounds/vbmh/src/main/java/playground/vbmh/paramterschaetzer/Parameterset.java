package playground.vbmh.paramterschaetzer;

public class Parameterset {
	public double betaMatsimScore, betaReserve, betaSOC, score, constant;

	public Parameterset(double betaMatsimScore, double betaReserve,
			double betaSOC, double constant) {
		super();
		this.betaMatsimScore = betaMatsimScore;
		this.betaReserve = betaReserve;
		this.betaSOC = betaSOC;
		this.constant= constant;
	}
	
	public double calcExp(double matsimScore, double reserve, double nSOC){
		return Math.exp(betaMatsimScore*matsimScore + betaReserve * reserve + betaSOC * nSOC);
	}
	
	public String toString(){
		return "const :"+this.constant+" mats :"+this.betaMatsimScore+" reserve :"+this.betaReserve+" SOC :"+this.betaSOC;
	}
}
