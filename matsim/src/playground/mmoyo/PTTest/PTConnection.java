package playground.mmoyo.PTTest;

import java.util.ArrayList;
import java.util.List;

public class PTConnection {
	private List<Stretch> stretches = new ArrayList<Stretch>();
	private double duration;
	private double length;
	private double score;
	
    /**
	 * @param stretches
	 * @param duration
	 * @param length
	 * @param score
	 */
	public PTConnection(List<Stretch> stretches, double duration, double length, double score) {
		this.stretches = stretches;
		this.duration = duration;
		this.length = length;
		this.score = score;
	}
    
	public PTConnection() {
	}
	
    public List<Stretch> getStretches() {
		return stretches;
	}
	public void setStretches(List<Stretch> stretches) {
		this.stretches = stretches;
	}
	public double getDuration() {
		return duration;
	}
	public void setDuration(double duration) {
		this.duration = duration;
	}
	public double getLength() {
		return length;
	}
	public void setLength(double length) {
		this.length = length;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	

}