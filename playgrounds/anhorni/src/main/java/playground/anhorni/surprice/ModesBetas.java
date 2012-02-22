package playground.anhorni.surprice;

import java.util.TreeMap;

public class ModesBetas {
	
	private TreeMap<String, Double> modesBetas = new TreeMap<String, Double>();
	
	public ModesBetas() {		
	}
	
	public ModesBetas(TreeMap<String, Double> modesBetas) {
		this.modesBetas = modesBetas;
	}
	
	public double getModeBeta(String mode) {
		return this.modesBetas.get(mode);
	}
	
	public void setModeBeta(String mode, double beta) {
		this.modesBetas.put(mode, beta);
	}
}
