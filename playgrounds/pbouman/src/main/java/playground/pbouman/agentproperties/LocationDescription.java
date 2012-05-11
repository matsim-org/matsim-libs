package playground.pbouman.agentproperties;

public class LocationDescription
{
	private double x;
	private double y;
	private String linkId;
	
	public LocationDescription(double x, double y)
	{
		this.x = x;
		this.y = y;
		this.linkId = null;
	}
	
	public LocationDescription(String linkId)
	{
		this.x = 0;
		this.y = 0;
		this.linkId = linkId;
	}
	
	public boolean describesLink()
	{
		return linkId != null;
	}
	
	public String getLinkId()
	{
		return linkId;
	}
	
	public double getX()
	{
		return x;
	}
	
	public double getY()
	{
		return y;
	}
	
	public String toString()
	{
		if (describesLink())
			return "<location link=\""+linkId+"\" />";
		else
			return "<location x=\""+x+"\" y=\""+y+"\" />";
	}
}
