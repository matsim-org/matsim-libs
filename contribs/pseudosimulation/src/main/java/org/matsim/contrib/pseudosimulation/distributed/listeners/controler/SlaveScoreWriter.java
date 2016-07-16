package org.matsim.contrib.pseudosimulation.distributed.listeners.controler;

import org.matsim.contrib.pseudosimulation.distributed.MasterControler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;

public class SlaveScoreWriter implements IterationEndsListener,
		ShutdownListener, StartupListener {
	private final MasterControler controler;
	private BufferedWriter out;
	final static int INDEX_WORST = 0;
	final static int INDEX_BEST = 1;
	final static int INDEX_AVERAGE = 2;
	final static int INDEX_EXECUTED = 3;

	public SlaveScoreWriter(MasterControler controler) {
		super();
		this.controler = controler;

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		double[][] history = controler.getSlaveScoreHistory();
		int idx = event.getIteration() - controler.getConfig().controler().getFirstIteration();
		try {
			out.write(event.getIteration() + "\t"
					+ history[INDEX_EXECUTED][idx] + "\t"
					+ history[INDEX_WORST][idx] + "\t"
					+ history[INDEX_AVERAGE][idx] + "\t"
					+ history[INDEX_BEST][idx] + "\n");
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// create chart when data of more than one iteration is available.
		if(idx<2){
			return;
		}
		XYLineChart chart = new XYLineChart("SlaveHandler Score Statistics",
				"iteration", "score");
		double[] iterations = new double[idx];
		for (int i = 0; i < idx; i++) {
			iterations[i] = i + controler.getConfig().controler().getFirstIteration()+1;
		}
		double[] values = new double[idx];
		double[] fullhist = new double[idx];
		int[] series = { INDEX_WORST, INDEX_BEST, INDEX_AVERAGE, INDEX_EXECUTED };
		String[] seriesNames = { "avg. worst score", "avg. best score",
				"avg. of plans' average score", "avg. executed score" };
		for (int s = 0; s < series.length; s++) {
			System.arraycopy(history[series[s]], 1, fullhist, 0,
					fullhist.length);
			int valuecounter = 0;
			for (int i =0;i<idx;i++) {
				values[valuecounter++] = fullhist[i];
			}
			chart.addSeries(seriesNames[s], iterations, values);

		}




		chart.addMatsimLogo();
		chart.saveAsPng(controler.getMATSimControler().getControlerIO().getOutputPath()
				+ "/slaveScoreStats.png", 1200, 800);

	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void notifyStartup(StartupEvent event) {
		String fileName = controler.getMATSimControler().getControlerIO().getOutputPath()
				+ "/slaveScoreStats.txt";
		this.out = IOUtils.getBufferedWriter(fileName);

		try {
			this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
			this.out.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}
}
