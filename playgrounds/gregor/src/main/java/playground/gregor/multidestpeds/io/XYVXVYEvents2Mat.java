package playground.gregor.multidestpeds.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;

public class XYVXVYEvents2Mat implements XYVxVyEventsHandler{

	
	private final Map<Id,Ped> red = new HashMap<Id,Ped>();
	private final Map<Id,Ped> green = new HashMap<Id,Ped>();
	
	double time = 0;
	private final List<Double> timeStamps = new ArrayList<Double>();
	
	public void write(String outputMat) {
		MatFileWriter writer = new MatFileWriter();
		
		
		List<Ped> red = new ArrayList<Ped>(this.red.values());
		List<Ped> green = new ArrayList<Ped>(this.green.values());
		
		
		double[][] times = new double[1][this.timeStamps.size()];
		int idx = 0;
		for (Double time : this.timeStamps) {
			times[0][idx++] = time;
		}
		
		double [][] redX = new double[this.timeStamps.size()][this.red.size()];
		double [][] redY = new double[this.timeStamps.size()][this.red.size()];
		double [][] redVX = new double[this.timeStamps.size()][this.red.size()];
		double [][] redVY = new double[this.timeStamps.size()][this.red.size()];
		
		double [][] greenX = new double[this.timeStamps.size()][this.green.size()];
		double [][] greenY = new double[this.timeStamps.size()][this.green.size()];
		double [][] greenVX = new double[this.timeStamps.size()][this.green.size()];
		double [][] greenVY = new double[this.timeStamps.size()][this.green.size()];
		
		for (int i = 0; i < this.timeStamps.size(); i++) {
			double time = times[0][i];
			
			for (int j = 0; j < red.size(); j++) {
				Ped p = red.get(j);
				XYVxVyEvent e = p.timeEvents.get(time);
				if (e == null) {
					redX[i][j] = Double.NaN;
					redY[i][j] = Double.NaN;
					redVX[i][j] = Double.NaN;
					redVY[i][j] = Double.NaN;
				} else {
					redX[i][j] = e.getX();
					redY[i][j] = e.getY();
					redVX[i][j] = e.getVX();
					redVY[i][j] = e.getVY();		
					
					if (i > 0 && j == 37) {
					double dx = redX[i][j] - redX[i-1][j];
					double dy = redY[i][j] - redY[i-1][j];
					double v = Math.hypot(dx, dy)*25;
					if (v > 2) {
						System.err.println(Math.hypot(dx, dy)*25);
					}}
				}
			}
			for (int j = 0; j < green.size(); j++) {
				Ped p = green.get(j);
				XYVxVyEvent e = p.timeEvents.get(time);
				if (e == null) {
					greenX[i][j] = Double.NaN;
					greenY[i][j] = Double.NaN;
					greenVX[i][j] = Double.NaN;
					greenVY[i][j] = Double.NaN;
				} else {
					greenX[i][j] = e.getX();
					greenY[i][j] = e.getY();
					greenVX[i][j] = e.getVX();
					greenVY[i][j] = e.getVY();					
				}
			}			
			
			
		}
		
		List<MLArray> mls = new ArrayList<MLArray>();
		MLArray mlRedX = new MLDouble("xr", redX);
		mls.add(mlRedX);
		MLArray mlRedY = new MLDouble("yr", redY);
		mls.add(mlRedY);
		MLArray mlRedVX = new MLDouble("vxr", redVX);
		mls.add(mlRedVX);
		MLArray mlRedVY = new MLDouble("vyr", redVY);
		mls.add(mlRedVY);
		MLArray mlGreenX = new MLDouble("xg", greenX);
		mls.add(mlGreenX);
		MLArray mlGreenY = new MLDouble("yg", greenY);
		mls.add(mlGreenY);
		MLArray mlGreenVX = new MLDouble("vxg", greenVX);
		mls.add(mlGreenVX);
		MLArray mlGreenVY = new MLDouble("vyg", greenVY);
		mls.add(mlGreenVY);
		MLArray time = new MLDouble("time_stamps", times);
		mls.add(time);
		try {
			writer.write(new File(outputMat), mls);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		double eTime = event.getTime();
		if (eTime > this.time) {
			this.timeStamps.add(eTime);
			this.time = eTime;
		}
		Id id = event.getPersonId();
		if (id.toString().contains("g")) {
			updatePed(this.green,event);
		} else if (id.toString().contains("r")){
			updatePed(this.red,event);
		}
		
		
	}
	
	private void updatePed(Map<Id, Ped> map, XYVxVyEvent event) {
		Ped ped = map.get(event.getPersonId());
		if (ped == null) {
			ped = new Ped();
			map.put(event.getPersonId(), ped);
		}
		ped.timeEvents.put(event.getTime(), event);
		
	}

	
	public static void main(String [] args) {
		String events = "/Users/laemmel/devel/gr90/output/ITERS/it.0/0.events.xml.gz";
		String outputMat = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/experimental_data/Dez2010/simulated/gr90_zanlungo.mat";
		
		EventsManager mgr = EventsUtils.createEventsManager();
		XYVXVYEvents2Mat handler = new XYVXVYEvents2Mat();
		mgr.addHandler(handler);
		new XYVxVyEventsFileReader(mgr).parse(events);
		handler.write(outputMat);
	}
	
	private static final class Ped {
		Map<Double,XYVxVyEvent> timeEvents = new HashMap<Double,XYVxVyEvent>();
	}
}
