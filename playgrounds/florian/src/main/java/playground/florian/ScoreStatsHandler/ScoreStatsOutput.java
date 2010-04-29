package playground.florian.ScoreStatsHandler;

import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.florian.JFreeTest.ScoreToChartTest;

public class ScoreStatsOutput implements ShutdownListener{
	
	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;
	
	private final String filename;
	private final boolean createChart;
	private final ScoreXMLWriter out = new ScoreXMLWriter();
	private double[][] history = null;
	
	public ScoreStatsOutput(String filename, boolean createChart){
		this.filename = filename;
		this.createChart = createChart;
	}


	public void notifyShutdown(ShutdownEvent event) {
		history = event.getControler().getScoreStats().getHistory();
		int firstIt = event.getControler().getFirstIteration();
		int lastIt = event.getControler().getLastIteration();
		int maxHistory = lastIt - firstIt;
		for (int i = 0; i<=maxHistory; i++){
			int it = i + firstIt;
			out.addScore(it, history[INDEX_AVERAGE][it], history[INDEX_BEST][it],history[INDEX_WORST][it], history[INDEX_EXECUTED][it]);
		}
		out.write(filename);
		if (createChart){
			JFreeChart chart = ScoreToChartTest.createChartFromXMLScore(filename);
			try {
				ChartUtilities.saveChartAsPNG(new File(filename + ".png"), chart, 800, 600);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
}
