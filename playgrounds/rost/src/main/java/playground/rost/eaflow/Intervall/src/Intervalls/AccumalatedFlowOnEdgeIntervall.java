package playground.rost.eaflow.Intervall.src.Intervalls;


public class AccumalatedFlowOnEdgeIntervall extends Intervall {
	protected int flow;
	
	public int getFlow()
	{
		return flow;
	}
	
	public void setFlow(int flow)
	{
		this.flow = flow;
	}
	
	public AccumalatedFlowOnEdgeIntervall(int lowBound, int highBound)
	{
		super(lowBound, highBound);
	}
	
	public AccumalatedFlowOnEdgeIntervall(Intervall intervall, int flow)
	{
		super(intervall.getLowBound(), intervall.getHighBound());
		this.flow = flow;
	}
}
