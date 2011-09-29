package playground.wdoering.debugvisualization.model;
import java.awt.Point;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;


public class Agent {
	
	public HashMap<Double,DataPoint>dataPoints = new HashMap<Double,DataPoint>();
	

	public HashMap<Double, DataPoint> getDataPoints()
	{
		return dataPoints;
	}

	public void setDataPoints(HashMap<Double, DataPoint> dataPoints)
	{
		this.dataPoints = dataPoints;
	}

	public void Agent ()
	{
		
	}
	
	public void addDataPoint(DataPoint dataPoint)
	{
		dataPoints.put(dataPoint.getTime(), dataPoint);
	}

	public void addDataPoint(Double time, Double posX, Double posY)
	{
		DataPoint dataPoint = new DataPoint(time, posX, posY);
		dataPoints.put(time,dataPoint);
	}

	public void addDataPoint(Double time, Double posX, Double posY, Double posZ)
	{
		DataPoint dataPoint = new DataPoint(time, posX, posY, posZ);
		dataPoints.put(time,dataPoint);
	}
	
//	public DataPoint getDataPoint(int index)
//	{
//		return (DataPoint)this.dataPoints.get(index);
//	}

	public DataPoint getDataPoint(double index)
	{
		return (DataPoint)this.dataPoints.get(index);
	}
	
}
