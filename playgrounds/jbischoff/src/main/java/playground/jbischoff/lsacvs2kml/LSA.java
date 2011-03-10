package playground.jbischoff.lsacvs2kml;

public class LSA {
	
	private String ShortName;
	private String LongName;
	private double xcord;
	private double ycord;
	public LSA(){}
	public LSA(String sn,String ln,double x,double y)
	{
		ShortName=sn;
		LongName=ln;
		xcord=x;
		ycord=y;
	}

	public String getShortName() {
		return ShortName;
	}

	public void setShortName(String shortName) {
		ShortName = shortName;
	}

	public String getLongName() {
		return LongName;
	}

	public void setLongName(String longName) {
		LongName = longName;
	}

	public double getXcord() {
		return xcord;
	}

	public void setXcord(double xcord) {
		this.xcord = xcord;
	}

	public double getYcord() {
		return ycord;
	}

	public void setYcord(double ycord) {
		this.ycord = ycord;
	}
	public String toString(){
		return(getLongName()+" "+getShortName()+" "+getXcord()+" "+getYcord()+"\n");
	}
	

}
