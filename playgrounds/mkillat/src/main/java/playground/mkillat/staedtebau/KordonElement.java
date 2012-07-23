package playground.mkillat.staedtebau;

public class KordonElement {


	
	String kennzeichen;
	double time;
	
	public KordonElement(String kennzeichen, double time){
		this.kennzeichen = kennzeichen;
		this.time=time;
	}

	@Override
	public String toString() {
		return "KordonElement [" +  "kennzeichen="
				+ kennzeichen + ", time=" + time + "]";
	}

	private String getKennzeichen() {
		return kennzeichen;
	}

	private void setKennzeichen(String kennzeichen) {
		this.kennzeichen = kennzeichen;
	}

	private double getTime() {
		return time;
	}

	private void setTime(double time) {
		this.time = time;
	}
	
	
	
	
}
