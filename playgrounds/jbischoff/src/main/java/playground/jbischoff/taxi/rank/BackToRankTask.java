package playground.jbischoff.taxi.rank;

import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.AbstractTask;

public class BackToRankTask extends AbstractTask implements DriveTask {

	private Arc arc;
	public BackToRankTask(int beginTime, int endTime, Arc arc) {
		super(beginTime, endTime);

		this.arc = arc;	}

	@Override
	public TaskType getType() {
		 return TaskType.DRIVE;
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
