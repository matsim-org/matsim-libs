package playground.wdoering.debugvisualization.model;

public class XYVxVyAgent extends Agent
{
	private String currentLinkID;
	
	public XYVxVyAgent()
	{
		super();
		this.currentLinkID = "-1";
	}

	public String getCurrentLinkID()
	{
		return currentLinkID;
	}

	public void setCurrentLinkID(String currentLinkID)
	{
		this.currentLinkID = currentLinkID;
	}
	
	
	
	

}
