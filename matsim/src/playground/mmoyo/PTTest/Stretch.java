package playground.mmoyo.PTTest;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class Stretch {
	
	private enum type {walk, wait, travel, transfer, detransfer };
	private String type;
	private double start;
	private Path path;
	private double end;

	/**
	 * @param type
	 * @param start
	 * @param nodeIni
	 * @param nodeFin
	 * @param end
	 */
	public Stretch(String type, double start, Path path, double end) {
		this.type = type;
		this.start = start;
		this.path = path;
		this.end = end;
	}
	
	public Stretch() {
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public double getStart() {
		return start;
	}

	public void setStart(double start) {
		this.start = start;
	}

	public double getEnd() {
		return end;
	}

	public void setEnd(double end) {
		this.end = end;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	
	
}