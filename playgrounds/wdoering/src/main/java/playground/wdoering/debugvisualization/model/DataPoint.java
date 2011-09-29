package playground.wdoering.debugvisualization.model;
import java.awt.Point;
import java.sql.Timestamp;


public class DataPoint {

	private Double time;
	private Double posX;
	private Double posY;
	private Double posZ;

	
	public Double getPosX() {
		return posX;
	}

	public void setPosX(Double posX) {
		this.posX = posX;
	}

	public Double getPosY() {
		return posY;
	}

	public void setPosY(Double posY) {
		this.posY = posY;
	}


	public Double getTime() {
		return time;
	}

	public void setTime(Double time) {
		this.time = time;
	}

	
	public DataPoint(Double time, Double posX, Double posY)
	{
		this.time = time;
		this.posX = posX;
		this.posY = posY;
		
	}
	
	public DataPoint(Double time, Double posX, Double posY, Double posZ)
	{
		this.time = time;
		this.posX = posX;
		this.posY = posY;
		this.posZ = posZ;
		
	}

	public DataPoint(Double posX, Double posY)
	{
		this.time = 0d;
		this.posX = posX;
		this.posY = posY;
	}
	
	
	
	
}
