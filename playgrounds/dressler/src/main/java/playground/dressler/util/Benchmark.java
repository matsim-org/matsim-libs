package playground.dressler.util;

import playground.dressler.Interval.EdgeInterval;
import playground.dressler.Interval.EdgeIntervals;
import playground.dressler.Interval.Interval;
import playground.dressler.Interval.VertexInterval;
import playground.dressler.Interval.VertexIntervals;

public class Benchmark {

	//Random rand = new MRandom(42);
	
	public static void main(final String[] args) {
		
		Benchmark bench = new Benchmark();
		
		int timehorizon = 500;
		int repeat = 5000;
		
		int retrieveFactor = 3;
		
			
		System.out.println("Splitting VertexIntervals");
		CPUTimer timer = new CPUTimer(); 
		
		for (int insert = 100; insert  < 2000; insert  *= 2) {
			int retrieve;
			retrieve = insert * retrieveFactor;
			
			timer.onoff();			
			for (int j = 0; j < repeat; j++) {				
			    bench.benchmarkVertexIntervals(insert , retrieve, timehorizon);			    
			}
			timer.onoff();
			System.out.println(timer + " for " + repeat + " repetitions, inserting " + insert + " retrieving " + retrieve);
		}
		
		timer.newiter();
		
		System.out.println("Splitting EdgeIntervals");
		for (int insert = 100; insert  < 2000; insert  *= 2) {
			int retrieve;
			retrieve = insert * retrieveFactor;
			
			timer.onoff();			
			for (int j = 0; j < repeat; j++) {
			     bench.benchmarkEdgeIntervals(insert , retrieve, timehorizon);
			}
		    timer.onoff();
				
			System.out.println(timer + " for " + repeat + " repetitions, inserting " + insert + " retrieving " + retrieve);
		}
		
		timer.newiter();
		
		System.out.println("Augmenting EdgeIntervals");
		for (int insert = 100; insert  < 2000; insert  *= 2) {
			int retrieve;
			retrieve = insert * retrieveFactor;
			
			timer.onoff();
			for (int j = 0; j < repeat; j++) {
			  bench.benchmarkFlow(insert , timehorizon);
			}
			timer.onoff();
			
			System.out.println(timer + " for " + repeat + " repetitions, inserting " + insert + " retrieving " + retrieve);
		}
        
		timer.newiter();
		
		System.out.println("some more elaborate things ...");
		{
			
			int repeatpadang = 4400 * 100; // # of vertices * repeat
			int degree = 3; // avg degree
			
			timer.onoff();
			for (int j = 0; j < repeatpadang; j++) {
				bench.padang(degree);					
			}
			timer.onoff();
			System.out.println(timer + " for " + repeatpadang + " repetitions of padang(" + degree + ")");
		}
	}
	
	public void benchmarkVertexIntervals(int insertions, int retrieval, int timehorizon) {
		
		VertexIntervals Data = new VertexIntervals(new VertexInterval(0, timehorizon));
		
		for (int i = 0; i < insertions; i++) {			
			int t = MyRandom.nextInt(timehorizon);
			Interval inter = Data.getIntervalAt(t);
			if (t != inter.getLowBound() && t != inter.getHighBound())
			  Data.splitAt(t);
		}		
		
		for (int i = 0; i < retrieval; i++) {
			int t = MyRandom.nextInt(timehorizon);
			Data.getIntervalAt(t);
		}
				
	}
	
	public void benchmarkEdgeIntervals(int insertions, int retrieval, int timehorizon) {
		
		EdgeIntervals Data = new EdgeIntervals(new EdgeInterval(0, timehorizon), 0, 0, null);
		
		for (int i = 0; i < insertions; i++) {			
			int t = MyRandom.nextInt(timehorizon);
			Interval inter = Data.getIntervalAt(t);
			if (t != inter.getLowBound() && t != inter.getHighBound())
			  Data.splitAt(t);
		}		
		
		for (int i = 0; i < retrieval; i++) {
			int t = MyRandom.nextInt(timehorizon);
			Data.getIntervalAt(t);
		}
			
	}
	
	public void benchmarkFlow(int augmentations, int timehorizon) {
		
		EdgeIntervals Data = new EdgeIntervals(new EdgeInterval(0, timehorizon), 0, augmentations, null);
		
		for (int i = 0; i < augmentations; i++) {			
			int t = MyRandom.nextInt(timehorizon);
			//int f = Data.getFlowAt(t);
			Data.augment(t, 1);
		}		
		
		
	}
	
	public void padang(int degree) {
		int timehorizon = 1700;
		int polls = 30; // per vertex, on avg
		int edgeinserts = 52;				
		
		// create some flow on an edge
		EdgeIntervals flow = new EdgeIntervals(new EdgeInterval(0, timehorizon), 0, edgeinserts, new Interval(0, timehorizon));
		for (int i = 0; i < edgeinserts; i++) {
			int t = MyRandom.nextInt(timehorizon);
			flow.augment(t, 1);
		}

		VertexIntervals VI = new VertexIntervals(new VertexInterval(0, timehorizon));
		
		// make an interesting vertex
		for (int i = 0; i < polls; i++) {
			int t = MyRandom.nextInt(timehorizon);
			Interval inter = VI.getIntervalAt(t);
			if (t != inter.getLowBound() && t != inter.getHighBound())
			  VI.splitAt(t);
		}
		
		// actual polls
		for (int d = 0; d < degree; d++) {
			for (int i = 0; i < polls; i++) {
				int t = MyRandom.nextInt(timehorizon);
				Interval inter = VI.getIntervalAt(t);
				// do something ... 
				flow.propagate(inter, true, false, timehorizon);
				//flow.getIntervalAt(inter.getLowBound());
			}
		}
		
	}
}
