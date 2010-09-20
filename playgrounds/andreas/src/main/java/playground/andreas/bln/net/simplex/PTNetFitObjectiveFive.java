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
import playground.andreas.optimization.Objective;
import playground.andreas.optimization.ParamPoint;
import playground.mzilske.bvg09.TransitControler;

public class PTNetFitObjectiveFive implements Objective{
	
	public static final String OBJECTIVE_NAME = "ptNetFit";

	public static final double EPSILON = 100.0;		// TODO, test what value is best for this param

	public static final int DIMENSION = 10;

	public static final int sF_1_idx = 0;
	public static final int sT_1_idx = 1;
	public static final int sF_2_idx = 2;
	public static final int sT_2_idx = 3;
	public static final int sF_3_idx = 4;
	public static final int sT_3_idx = 5;
	public static final int sF_4_idx = 6;
	public static final int sT_4_idx = 7;
	public static final int sF_5_idx = 8;
	public static final int sT_5_idx = 9;

	public static final String sF_1_name = "scale factor 1";
	public static final String sT_1_name = "start time 1";
	public static final String sF_2_name = "scale factor 2";
	public static final String sT_2_name = "start time 2";
	public static final String sF_3_name = "scale factor 3";
	public static final String sT_3_name = "start time 3";
	public static final String sF_4_name = "scale factor 4";
	public static final String sT_4_name = "start time 4";
	public static final String sF_5_name = "scale factor 5";
	public static final String sT_5_name = "start time 5";

	public Random rnd;	
	private String tmpDir;
	
	private BufferedWriter allWriter;
	private BufferedWriter bestWriter;
	private double bestScore = Double.MAX_VALUE;
	private int runCouter = 0;
	
	private ParamPoint[] initPPoints;
	private HashMap<ParamPoint, Double> knownPoints = new HashMap<ParamPoint, Double>();

	private final static Logger log = Logger.getLogger(PTNetFitObjectiveFive.class);
	
	
	public PTNetFitObjectiveFive(String tmpDir, int iteration){
		this.tmpDir = tmpDir;
		this.rnd = MatsimRandom.getLocalInstance();
		this.initPPoints = new ParamPoint[DIMENSION+1];
		try {
			this.allWriter = new BufferedWriter(new FileWriter(new File(this.tmpDir + iteration +  ".all_results.txt")));
			this.allWriter.write("Iteration, Score, " + sT_1_name + ", " + sF_1_name + ", "
					+ sT_2_name + ", " + sF_2_name + ", " + sT_3_name + ", " + sF_3_name + ", "
					+ sT_4_name + ", " + sF_4_name + ", " + sT_5_name + ", " + sF_5_name);
			this.allWriter.newLine();
			this.allWriter.flush();
			
			this.bestWriter = new BufferedWriter(new FileWriter(new File(this.tmpDir + iteration + ".best_results.txt")));
			this.bestWriter.write("Iteration, Score, " + sT_1_name + ", " + sF_1_name + ", "
					+ sT_2_name + ", " + sF_2_name + ", " + sT_3_name + ", " + sF_3_name + ", "
					+ sT_4_name + ", " + sF_4_name + ", " + sT_5_name + ", " + sF_5_name);
			this.bestWriter.newLine();
			this.bestWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	@Override
	public ParamPoint getInitialParamPoint(int index) {
		if (index > DIMENSION) {
			log.warn("Initial paramPoint " + index + " was requested, but we only have 10 Dimensions. Returning Initial paramPoint 0.");
			return this.initPPoints[0];
	  }
		return this.initPPoints[index];		
	}

	@Override
	public ParamPoint getNewParamPoint() {
		ParamPoint p = new ParamPoint(DIMENSION);
		return p;
	}

	@Override
	public TreeMap<String, Double> getParamMap(ParamPoint p) {
		TreeMap<String, Double> map = new TreeMap<String, Double>();
		map.put(sF_1_name, Double.valueOf(p.getValue(sF_1_idx)));
		map.put(sT_1_name, Double.valueOf(p.getValue(sT_1_idx)));
		map.put(sF_2_name, Double.valueOf(p.getValue(sF_2_idx)));
		map.put(sT_2_name, Double.valueOf(p.getValue(sT_2_idx)));
		map.put(sF_3_name, Double.valueOf(p.getValue(sF_3_idx)));
		map.put(sT_3_name, Double.valueOf(p.getValue(sT_3_idx)));
		map.put(sF_4_name, Double.valueOf(p.getValue(sF_4_idx)));
		map.put(sT_4_name, Double.valueOf(p.getValue(sT_4_idx)));
		map.put(sF_5_name, Double.valueOf(p.getValue(sF_5_idx)));
		map.put(sT_5_name, Double.valueOf(p.getValue(sT_5_idx)));
		return map;
	}

	@Override
	public double getResponse(ParamPoint p) {
		
		if(!this.knownPoints.containsKey(p)){
			
			this.runCouter++;
			
			// write change events according to parameter set
			ScaleFactorToChangeEvent changeEvents = new ScaleFactorToChangeEvent(this.tmpDir + "network.xml", this.tmpDir + "changeEvents.xml");
			
			for (int i = 0; i < PTNetFitObjectiveFive.DIMENSION; i += 2) {
				changeEvents.addTimeScaleFactorSet(Time.writeTime(p.getValue(i+1)), p.getValue(i));
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
			delHandler.addStartTime(p.getValue(sT_1_idx));
			delHandler.addStartTime(p.getValue(sT_2_idx));
			delHandler.addStartTime(p.getValue(sT_3_idx));
			delHandler.addStartTime(p.getValue(sT_4_idx));
			delHandler.addStartTime(p.getValue(sT_5_idx));
			delHandler.readEvents(this.tmpDir + "output/ITERS/it.0/0.events.xml.gz");

			// Most important part - calculating the score	
			double score = 0.0;
			
			for (DelayHandler handler : delHandler.getHandler()) {
				score += Math.pow(handler.negArrivalDelay.getAverageDelay(),2 ) + Math.pow(handler.posDepartureDelay.getAverageDelay(), 2);
			}
			
			this.knownPoints.put(p, Double.valueOf(score));
									
			try {
				this.allWriter.write(this.runCouter + ", " + score + ", "
						+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_1_idx)) + ", "
						+ p.getValue(PTNetFitObjectiveFive.sF_1_idx) + ", "
						+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_2_idx)) + ", "
						+ p.getValue(PTNetFitObjectiveFive.sF_2_idx) + ", "
						+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_3_idx)) + ", "
						+ p.getValue(PTNetFitObjectiveFive.sF_3_idx) + ", "
						+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_4_idx)) + ", "
						+ p.getValue(PTNetFitObjectiveFive.sF_4_idx) + ", "
						+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_5_idx))  + ", "
						+ p.getValue(PTNetFitObjectiveFive.sF_5_idx) );
						
				this.allWriter.newLine();
				this.allWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			if(score < this.bestScore){
				this.bestScore = score;
				
				try {
					this.bestWriter.write(this.runCouter + ", " + score + ", "
							+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_1_idx)) + ", "
							+ p.getValue(PTNetFitObjectiveFive.sF_1_idx) + ", "
							+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_2_idx)) + ", "
							+ p.getValue(PTNetFitObjectiveFive.sF_2_idx) + ", "
							+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_3_idx)) + ", "
							+ p.getValue(PTNetFitObjectiveFive.sF_3_idx) + ", "
							+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_4_idx)) + ", "
							+ p.getValue(PTNetFitObjectiveFive.sF_4_idx) + ", "
							+ Time.writeTime(p.getValue(PTNetFitObjectiveFive.sT_5_idx)) + ", "
							+ p.getValue(PTNetFitObjectiveFive.sF_5_idx) );
							
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
		if(p.getValue(sF_1_idx) <= 0.0 
				|| p.getValue(sF_2_idx) <= 0.0
				|| p.getValue(sF_3_idx) <= 0.0
				|| p.getValue(sF_4_idx) <= 0.0
				|| p.getValue(sF_5_idx) <= 0.0){
			return false;
		}
		
		if(p.getValue(sT_1_idx) >= p.getValue(sT_2_idx)
				|| p.getValue(sT_2_idx) >= p.getValue(sT_3_idx)
				|| p.getValue(sT_3_idx) >= p.getValue(sT_4_idx)
				|| p.getValue(sT_4_idx) >= p.getValue(sT_5_idx)){
			return false;
		}
		if(p.getValue(sT_1_idx) < 0.0
				|| p.getValue(sT_2_idx) < 0.0
				|| p.getValue(sT_3_idx) < 0.0
				|| p.getValue(sT_4_idx) < 0.0
				|| p.getValue(sT_5_idx) < 0.0 ){
			return false;
		}
		if(p.getValue(sT_1_idx) > 30 * 3600
				|| p.getValue(sT_2_idx) > 30 * 3600
				|| p.getValue(sT_3_idx) > 30 * 3600
				|| p.getValue(sT_4_idx) > 30 * 3600
				|| p.getValue(sT_5_idx) > 30 * 3600 ){
			return false;
		}		
		
		return true;
	}

	@Override
	public void setInitParamPoint(ParamPoint p, int i) {
		if ((0 > i) || (i > DIMENSION)) {
			Gbl.errorMsg("index " + i + " not allowed!");
		}
		this.initPPoints[i] = p;		
	}

}
