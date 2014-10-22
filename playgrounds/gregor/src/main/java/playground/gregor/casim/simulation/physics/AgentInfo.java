package playground.gregor.casim.simulation.physics;

public class AgentInfo {

	private double rho;
	private int[] mySpacingsFront;
	private int[] mySpacingsBehind;
	private int[] theirSpacingsFront;
	private int[] theirSpacingsBehind;
	private CAAgent agent;
	private String situation;
	private String scene;

	public AgentInfo(CAAgent caAgent) {
		this.agent = caAgent;
	}
	public void setRho(double rho) {
		this.rho = rho;
	}
	public double getRho() {
		return this.rho;
	}
	public void setMySpacings(int[] mySpacingsFront, int[] mySpacingsBehind) {
		this.mySpacingsFront = mySpacingsFront;
		this.mySpacingsBehind = mySpacingsBehind;
	}
	public int [] getMySpacingsFront() {
		return this.mySpacingsFront;
	}
	public int [] getMySpacingsBehind() {
		return this.mySpacingsBehind;
	}
	public void setTheirSpacings(int[] theirSpacingsFront,
			int[] theirSpacingsBehind) {
		this.theirSpacingsFront = theirSpacingsFront;
		this.theirSpacingsBehind = theirSpacingsBehind;
	}
	public int [] getTheirSpacingsFront(){
		return this.theirSpacingsFront;
	}
	public int [] getTheirSpacingsBehind(){
		return this.theirSpacingsBehind;
	}
	
	public String getScene(){
		return this.scene;
	}
	public void makeInfo(CAAgent[] parts) {
		int dir = agent.getDir();
		int pos = agent.getPos();
		int mBehind = Math.max(mySpacingsBehind[0], theirSpacingsBehind[0]);
		int mFront = Math.max(mySpacingsFront[0], theirSpacingsFront[0]);
		
		int from,to;
		if (dir == -1) {
			from = pos-mFront;
			to = pos+mBehind;
		} else {
			from = pos-mBehind;
			to = pos+mFront;
		}
		if (from < 0) {
			from = 0;
		}
		if (to > parts.length-1) {
			to = parts.length-1;
		}
		StringBuffer buf = new StringBuffer();
		buf.append("...");
		for (int i = from; i <= to; i++) {
			if (i == pos) {
				buf.append("*");
			} else if (parts[i] != null) {
				if (parts[i].getDir() == dir) {
					buf.append("o");
				} else {
					buf.append("x");
				}
			} else {
				buf.append("_");
			}
		}
		buf.append("...");
		this.scene = buf.toString();
	}
}
