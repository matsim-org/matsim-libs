package playground.jbischoff.taxi.rank;

import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.AbstractTask;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.schedule.TaxiDriveTask;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.schedule.TaxiDriveTask.TaxiDriveType;

public class BackToRankTask extends TaxiDriveTask implements DriveTask {
	 private final TaxiDriveType driveType;

	
	private Arc arc;
	
	
	public BackToRankTask(int beginTime, int endTime, Arc arc) {
		super(beginTime, endTime, arc);
		
		//the following is possibly nonsense, as a drive to rank is clearly not a pickup task. 
		//But it works for the time being as expected
	
		driveType = TaxiDriveType.PICKUP;
		this.arc = arc;	}

	@Override
	public TaskType getType() {
		 return TaskType.DRIVE;
	}
	@Override
	public TaxiDriveType getDriveType()
	    {
	        return driveType;
	    }
	
	
	@Override
	public Arc getArc() {
		// TODO Auto-generated method stub
		return arc;
	}

	   @Override
	    public String toString()
	    {
	        return "DtoRank(@" + arc.getFromVertex().getId() + "->@" + arc.getToVertex().getId() + ")"
	                + commonToString();
	    }
}
