package playground.andreas.bln.net.simplex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Time;

import playground.andreas.bln.net.simplex.SimplexDelayHandler.DelayHandler;
import playground.jhackney.optimization.Objective;
import playground.jhackney.optimization.ParamPoint;
import playground.mzilske.bvg09.TransitControler;

public class PTNetFitObjectiveN implements Objective{
	
	public static final String OBJECTIVE_NAME = "ptNetFit";

	public static final double EPSILON = 100.0;		// TODO, test what value is best for this param

	public final int DIMENSION;
	
	// even entries contain time, odd contain scale factor 

	public Random rnd;	
	private String tmpDir;
	
	private BufferedWriter allWriter;
	private BufferedWriter bestWriter;
	private double bestScore = Double.MAX_VALUE;
	private int runCouter = 0;
	
	private ParamPoint[] initPPoints;
	private HashMap<ParamPoint, Double> knownPoints = new HashMap<ParamPoint, Double>();

	private final static Logger log = Logger.getLogger(PTNetFitObjectiveN.class);
	
	
	public PTNetFitObjectiveN(String tmpDir, int dimension, int iteration){
		this.tmpDir = tmpDir;
		this.DIMENSION = dimension;
		this.rnd = MatsimRandom.getLocalInstance();
		this.initPPoints = new ParamPoint[this.DIMENSION+1];
		try {
			this.allWriter = new BufferedWriter(new FileWriter(new File(this.tmpDir + "results/" + iteration +  ".all_results.txt")));
			StringBuffer stringbuffer = new StringBuffer();
			for (int i = 0; i < this.DIMENSION / 2; i++) {
				stringbuffer.append(", time " + i);
				stringbuffer.append(", factor " + i);
			}
			this.allWriter.write("Iteration, Score" + stringbuffer);
			this.allWriter.newLine();
			this.allWriter.flush();
			
			this.bestWriter = new BufferedWriter(new FileWriter(new File(this.tmpDir + "results/"  + iteration + ".best_results.txt")));
			this.bestWriter.write("Iteration, Score" + stringbuffer);
			this.bestWriter.newLine();
			this.bestWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	@Override
	public ParamPoint getInitialParamPoint(int index) {
		if (index > this.DIMENSION) {
			log.warn("Initial paramPoint " + index + " was requested, but we only have " + this.DIMENSION + " dimensions. Returning initial paramPoint 0.");
			return this.initPPoints[0];
	  }
		return this.initPPoints[index];		
	}

	@Override
	public ParamPoint getNewParamPoint() {
		ParamPoint p = new ParamPoint(this.DIMENSION);
		return p;
	}

	@Override
	public TreeMap<String, Double> getParamMap(ParamPoint p) {
		TreeMap<String, Double> map = new TreeMap<String, Double>();
		for (int i = 0; i < this.DIMENSION / 2; i++) {
			map.put("time " + i, Double.valueOf(p.getValue(2 * i)));
			map.put("factor " + i, Double.valueOf(p.getValue(2 * i + 1)));
		}		
		return map;
	}

	@Override
	public double getResponse(ParamPoint p) {
		
		if(!this.knownPoints.containsKey(p)){
			
			this.runCouter++;
			
			// write change events according to parameter set
			ScaleFactorToChangeEvent changeEvents = new ScaleFactorToChangeEvent(this.tmpDir + "network.xml", this.tmpDir + "changeEvents.xml");
			
			for (int i = 0; i < this.DIMENSION; i += 2) {
				changeEvents.addTimeScaleFactorSet(Time.writeTime(p.getValue(i)), p.getValue(i+1));
			}
			try {
				changeEvents.writeEvents();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// differentiate with respect to parameters
			TransitControler.main(new String[] {this.tmpDir + "config.xml"});

			// evaluate
			SimplexDelayHandler delHandler = new SimplexDelayHandler();
			for (int i = 0; i < this.DIMENSION; i += 2) {
				delHandler.addStartTime(p.getValue(i));
			}		
			delHandler.readEvents(this.tmpDir + "output/ITERS/it.0/0.events.xml.gz");

			// Preprocessing eval data
			int nPostiveEntries = 0;
			double sumPositiveEntries = 0;
			
			int nNegativeEntries = 0;
			double sumNegativeEntries = 0;	
			
			// Most important part - calculating the score	
			double score = 0.0;
			
			for (DelayHandler handler : delHandler.getHandler()) {
//				nPostiveEntries += handler.posDepartureDelay.getNumberOfEntries();
//				sumPositiveEntries += handler.posDepartureDelay.getAccumulatedDelay();
//				
//				nNegativeEntries += handler.negArrivalDelay.getNumberOfEntries();
//				sumNegativeEntries += handler.negArrivalDelay.getAccumulatedDelay();
//				
////			score += Math.sqrt(Math.pow(handler.negArrivalDelay.getAverageDelay(), 2) + Math.pow(handler.posDepartureDelay.getAverageDelay(), 2));
				score += Math.pow(handler.posDepartureDelay.getAverageDelay(), 2) + Math.pow(handler.negArrivalDelay.getAverageDelay(), 2);
			}
			
//			
//			score = 0.5 * (Math.abs(sumPositiveEntries) / nPostiveEntries + Math.abs(sumNegativeEntries) / nNegativeEntries);
			score = Math.sqrt(score / (delHandler.getHandler().size() * 2.0));
			
			// end of scoring
			
			this.knownPoints.put(p, Double.valueOf(score));
									
			try {
				StringBuffer stringbuffer = new StringBuffer();
				for (int i = 0; i < this.DIMENSION / 2; i++) {
					stringbuffer.append(", " + Time.writeTime(p.getValue(2 * i)));
					stringbuffer.append(", " + p.getValue(2 * i + 1));
				}
				this.allWriter.write(this.runCouter + ", " + score + stringbuffer);
						
				this.allWriter.newLine();
				this.allWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			if(score < this.bestScore){
				this.bestScore = score;
				
				try {
					StringBuffer stringbuffer = new StringBuffer();
					for (int i = 0; i < this.DIMENSION / 2; i++) {
						stringbuffer.append(", " + Time.writeTime(p.getValue(2 * i)));
						stringbuffer.append(", " + p.getValue(2 * i + 1));
					}
					this.bestWriter.write(this.runCouter + ", " + score + stringbuffer);
							
					this.bestWriter.newLine();
					this.bestWriter.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
				
		return this.knownPoints.get(p).doubleValue();		
	}
	
	
	@Override
	public boolean isValidParamPoint(ParamPoint p) {
		
		// check, if all scale factors are positive, non zero
		for (int i = 0; i < this.DIMENSION / 2; i++) {
			if(p.getValue(2 * i + 1) <= 0.0){
				return false;
			}
		}
		
		// check, if timeslots are in ascending order
		double lastTime = 0.0;
		for (int i = 0; i < this.DIMENSION / 2; i++) {
			if(lastTime >= p.getValue(2 * i)){
				return false;
			}
			lastTime = p.getValue(2 * i);
		}
		
		// check, if all timeslots are within day (lower bound)
		for (int i = 0; i < this.DIMENSION / 2; i++) {
			if(p.getValue(2 * i) < 0.0){
				return false;
			}
		}
		
		// check, if all timeslots are within day (upper bound)
		for (int i = 0; i < this.DIMENSION / 2; i++) {
			if(p.getValue(2 * i) > 30 * 3600){
				return false;
			}
		}
				
		return true;
	}

	@Override
	public void setInitParamPoint(ParamPoint p, int i) {
		if ((0 > i) || (i > this.DIMENSION)) {
			Gbl.errorMsg("index " + i + " not allowed!");
		}
		this.initPPoints[i] = p;		
	}

}
