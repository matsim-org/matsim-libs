package playground.gregor.sim2d_v3.simulation.floor;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class PhysicalAgentRepresentation {

	public static final double AGENT_WEIGHT = 80;
//	protected static final double AGENT_DIAMETER = 0.5;
	private final double diameter; 
	protected final double maxV = 2.;

	public PhysicalAgentRepresentation() {
		super();
		this.diameter = .4;//0.4 + 0.2*MatsimRandom.getRandom().nextDouble();
	}

	abstract public void update(double v, double alpha, Coordinate pos);
	
	public double getAgentDiameter() {
		return this.diameter;
	}

	abstract public void translate(Coordinate pos);

}