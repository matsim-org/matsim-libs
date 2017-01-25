package playground.jbischoff.taxibus.algorithm.tubs.datastructure;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

public class StateLookupTable implements IterationEndsListener, ShutdownListener, StateSpace {

	private ArrayList<State> experiencedStates;
	private double[][] valueMatrix;
	private double[][] occurenceMatrix;
	private Map<Integer,Integer> bookingsPerIteration = new TreeMap<>();
	
	private int bookingCounter = 0;
	private double currentMatrixStartTime;
	private double currentMatrixEndTime;
	private int bins;
	private double binsize;
	private final String outputDirectory;

	public StateLookupTable(double startTime, double endTime, double binSizeInSeconds, double maximumTourDuration,
			String outputputDir) {
		this.experiencedStates = new ArrayList<>();
		this.outputDirectory = outputputDir;
		double runtime = endTime - startTime;
		int bins = (int) (runtime / binSizeInSeconds);
		this.bins = bins;
		this.binsize = binSizeInSeconds;
		this.currentMatrixEndTime = endTime;
		this.currentMatrixStartTime = startTime;
		Logger.getLogger(getClass()).info("LookupTable runtime: " + runtime + " bins: " + bins);
		valueMatrix = new double[bins][];
		occurenceMatrix = new double[bins][];
		for (int i = 0; i < bins; i++) {
			int slacksize = bins - i;
			valueMatrix[i] = new double[slacksize];
			occurenceMatrix[i] = new double[slacksize];

			for (int ii = 0; ii < slacksize; ii++) {
				valueMatrix[i][ii] = 100.0;
				occurenceMatrix[i][ii] = 0.0;
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration()<=1000) {
			recalculateMatrix();
			}
		this.bookingsPerIteration.put(event.getIteration(), this.bookingCounter);
		this.experiencedStates.clear();
		this.bookingCounter = 0;
		writeMatrices(this.outputDirectory+"/occurences.txt", this.outputDirectory+"/values.txt", event.getIteration());
		
	}

	/**
	 * 
	 */
	private void recalculateMatrix() {

		for (State state : this.experiencedStates) {
			int currentIterationValue = this.bookingCounter - state.confirmations - 1;
			if (currentIterationValue<0) currentIterationValue = 0;
			try{
			double oldValue = this.valueMatrix[state.time][state.slack];
			double occurences = this.occurenceMatrix[state.time][state.slack];
			double newValue = ((occurences * oldValue) + currentIterationValue) / (occurences + 1);
			this.valueMatrix[state.time][state.slack] = newValue;
			this.occurenceMatrix[state.time][state.slack]++;}
			catch (ArrayIndexOutOfBoundsException e){}
		}

	}

	@Override
	public double getValue(double time, double slack) {
		double value = -1;
		try{
		 value = this.valueMatrix[getTimeBin(time)][getSlackBin(slack)];
		}
		catch (IndexOutOfBoundsException e){
			
		}
		Logger.getLogger(getClass()).info("time " + time + " slack " + slack);
		Logger.getLogger(getClass()).info(this.valueMatrix[getTimeBin(time)].length);
		Logger.getLogger(getClass()).info(value);
		return value;
	}

	private int getTimeBin(double time) {
		if ((time > currentMatrixEndTime) | (time < currentMatrixStartTime)) {
			throw new RuntimeException("out of time bin");
		}
		double timefromStart = time - currentMatrixStartTime;
		int bin = (int) (timefromStart / this.binsize) -1;
		if (bin < 0)
			bin = 0;
		return bin;
	}

	private int getSlackBin(double slack) {
		if (slack > (currentMatrixEndTime - currentMatrixStartTime)) {
			throw new RuntimeException("out of slack bin");
		}
		int bin = (int) (slack / binsize) - 1;
		if (bin < 0)
			bin = 0;
		return bin;
	}

	@Override
	public double getCurrentLastArrivalTime(double now) {
		// TODO Auto-generated method stub
		return 8 * 3600;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateSpace#
	 * addExperiencedTimeSlackTuple(double, double)
	 */
	@Override
	public void addExperiencedTimeSlack(double time, double slack, int confirmations) {
		State experiencedState = new State(getTimeBin(time), getSlackBin(slack), confirmations);

		for (State oldState : experiencedStates) {
			if ((oldState.time == experiencedState.time) && (oldState.slack == experiencedState.slack)
					&& (oldState.confirmations == experiencedState.confirmations)) {
				return;
				// we don't want to add the same thing twice
			}
		}
		this.experiencedStates.add(experiencedState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateSpace#
	 * incBookingCounter()
	 */
	@Override
	public void incBookingCounter() {
		this.bookingCounter++;
	}

	private void writeMatrices(String occurencesFile, String valuesFile, int iter) {
		writeMatrix(this.valueMatrix, valuesFile, iter);
		writeMatrix(this.occurenceMatrix, occurencesFile, iter);
	}

	/**
	 * @param valueMatrix2
	 * @param iterString
	 * @param valuesFile
	 */
	private void writeMatrix(double[][] matrix, String valuesFile, int iter) {
		String iterString = "====Iteration " + iter + " =====";
		BufferedWriter bw;
		if (iter == 0) {
			bw = IOUtils.getBufferedWriter(valuesFile);
		} else {
			bw = IOUtils.getAppendingBufferedWriter(valuesFile);
		}
		try {
			bw.write(iterString);
			bw.newLine();
			int height = matrix[0].length - 1;
			for (int i = height; i >= 0; i--) {
				bw.write(Integer.toString(i));
				for (int z = 0; z < matrix.length - 1; z++) {
					if (matrix[z].length > i) {
						bw.write("\t" + matrix[z][i]);
					} else
						break;
				}
				bw.newLine();
			}
			for (int z = 0; z < matrix.length - 1; z++) {
				bw.write("\t" + z);
			}
			bw.newLine();

			bw.flush();
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.ShutdownListener#notifyShutdown(org.matsim.core.controler.events.ShutdownEvent)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writeBookingStats("bookingStats.txt");
	}

	/**
	 * 
	 */
	private void writeBookingStats(String file) {
		BufferedWriter bw = IOUtils.getBufferedWriter(this.outputDirectory+"/"+file);
		try {
			bw.write("Iteration\tAcceptedBookings");
			for (Entry<Integer, Integer> e : this.bookingsPerIteration.entrySet()){
				bw.newLine();
				bw.write(e.getKey()+"\t"+e.getValue());
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateSpace#acceptableStartTime(double)
	 */
	@Override
	public boolean acceptableStartTime(double now) {
		if ((now > currentMatrixEndTime) | (now < currentMatrixStartTime)) {
			return false;
		}		else 
			return true;
	}
}
