package org.matsim.freight.receiver;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * Generates score stats for receivers. This is adapted from "org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats" by mrieser.
 *
 * @author wlbean
 */
final class ReceiverScoreStats implements StartupListener, IterationEndsListener, ShutdownListener {
	public static final String RECEIVER_STATS_CSV = "/receiver_stats.csv";
	public static final String CARRIER_PLANS_XML = ".carrierPlans.xml";
	public static final String RECEIVER_PLANS_XML = ".receivers.xml";
	@Inject
	Scenario sc;

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	private BufferedWriter out;

	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;


	private final static Logger log = LogManager.getLogger(ReceiverScoreStats.class);

	/**
	 * Creates a new ScoreStats instance.
	 */
	public ReceiverScoreStats() throws UncheckedIOException {
		/*FIXME Incorporate into ConfigGroup. */
		this.createPNG = true;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		String fileName = sc.getConfig().controller().getOutputDirectory() + ReceiverUtils.FILENAME_RECEIVER_SCORES;
		if (fileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
			this.out = IOUtils.getBufferedWriter(fileName);
		} else {
			this.out = IOUtils.getBufferedWriter(fileName + ".txt");
		}
		try {
			this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.minIteration = event.getServices().getConfig().controller().getFirstIteration();
		int maxIter = event.getServices().getConfig().controller().getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 5000) iterations = 5000; // limit the history size
		this.history = new double[4][iterations + 1];

		/* Write headings of receiver stats file */
		writeHeadings(sc);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		/* Write the standard scoring statistics. */
		writeIterationScores(event);

		/* Write receiver-specific outputs */
		recordReceiverStats(event);

		/* Write the carrier and receiver plans at specific iterations */
		if ((event.getIteration() + 1) % (ConfigUtils.addOrGetModule(sc.getConfig(), ReceiverConfigGroup.class).getReceiverReplanningInterval()) != 0)
			return;
		String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());
		CarriersUtils.writeCarriers(CarriersUtils.getCarriers(sc), dir + File.separator + event.getIteration() + CARRIER_PLANS_XML);
		new ReceiversWriter(ReceiverUtils.getReceivers(sc)).write(dir + File.separator + event.getIteration() + RECEIVER_PLANS_XML);
	}

	private void writeIterationScores(IterationEndsEvent event) {
		String fileName = sc.getConfig().controller().getOutputDirectory() + ReceiverUtils.FILENAME_RECEIVER_SCORES;

		double sumScoreWorst = 0.0;
		double sumScoreBest = 0.0;
		double sumAvgScores = 0.0;
		double sumExecutedScores = 0.0;
		int nofScoreWorst = 0;
		int nofScoreBest = 0;
		int nofAvgScores = 0;
		int nofExecutedScores = 0;

		for (Receiver receiver : ReceiverUtils.getReceivers(sc).getReceivers().values()) {
			ReceiverPlan worstPlan = null;
			ReceiverPlan bestPlan = null;
			double worstScore = Double.POSITIVE_INFINITY;
			double bestScore = Double.NEGATIVE_INFINITY;
			double sumScores = 0.0;
			double cntScores = 0;

			/*
			 * FIXME This probably requires some re-doing. I (JWJ) changed
			 * the code so that each PLAN is scored... but I think it should
			 * probable just be the SELECTED plan that is scored, right?
			 * And also, it should just aggregate the plan's score from
			 * the ReceiverOrder scores, right... that's what I've done
			 * here for now.
			 */
			for (ReceiverPlan plan : receiver.getPlans()) {
				Double score = plan.getScore();
				if (score == null) {
					continue;
				}

				/* Worst plan */
				if (worstPlan == null) {
					worstPlan = plan;
					worstScore = score;
				} else if (score < worstScore) {
					worstPlan = plan;
					worstScore = score;
				}

				/* Best plan */
				if (bestPlan == null) {
					bestPlan = plan;
					bestScore = score;
				} else if (score > bestScore) {
					bestPlan = plan;
					bestScore = score;
				}

				/* For calculating the average scores */
				sumScores += score;
				cntScores++;

				if (receiver.getSelectedPlan() == plan) {
					sumExecutedScores += score;
					nofExecutedScores++;
				}
			}

			if (worstPlan != null) {
				nofScoreWorst++;
				sumScoreWorst += worstScore;
			}
			if (bestPlan != null) {
				nofScoreBest++;
				sumScoreBest += bestScore;
			}
			if (cntScores > 0) {
				sumAvgScores += (sumScores / cntScores);
				nofAvgScores++;
			}
		}
		log.info("-- Receiver agent scoring:");
		log.info("    avg. score of the executed plan of each agent: " + (sumExecutedScores / nofExecutedScores));
		log.info("       avg. score of the worst plan of each agent: " + (sumScoreWorst / nofScoreWorst));
		log.info("            avg. of the avg. plan score per agent: " + (sumAvgScores / nofAvgScores));
		log.info("        avg. score of the best plan of each agent: " + (sumScoreBest / nofScoreBest));

		try {
			this.out.write(event.getIteration() + "\t" + (sumExecutedScores / nofExecutedScores) + "\t" +
				(sumScoreWorst / nofScoreWorst) + "\t" + (sumAvgScores / nofAvgScores) + "\t" + (sumScoreBest / nofScoreBest) + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_WORST][index] = (sumScoreWorst / nofScoreWorst);
			this.history[INDEX_BEST][index] = (sumScoreBest / nofScoreBest);
			this.history[INDEX_AVERAGE][index] = (sumAvgScores / nofAvgScores);
			this.history[INDEX_EXECUTED][index] = (sumExecutedScores / nofExecutedScores);

			if (this.createPNG && event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history[INDEX_WORST], 0, values, 0, index + 1);
				chart.addSeries("avg. worst score", iterations, values);
				System.arraycopy(this.history[INDEX_BEST], 0, values, 0, index + 1);
				chart.addSeries("avg. best score", iterations, values);
				System.arraycopy(this.history[INDEX_AVERAGE], 0, values, 0, index + 1);
				chart.addSeries("avg. of plans' average score", iterations, values);
				System.arraycopy(this.history[INDEX_EXECUTED], 0, values, 0, index + 1);
				chart.addSeries("avg. executed score", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(fileName + ".png", 1000, 600);
			}
			if (index == (this.history[0].length - 1)) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void writeHeadings(Scenario sc) {
		try(BufferedWriter bw = IOUtils.getBufferedWriter(sc.getConfig().controller().getOutputDirectory() + RECEIVER_STATS_CSV)) {
			bw.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
				"iteration",
				"receiver_id",
				"score",
				"timewindow_start",
				"timewindow_end",
				"timewindow_duration",
				"order_id",
				"volume",
				"frequency",
				"serviceduration",
				"collaborate",
				"grandCoalitionMember"));
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write initial headings");
		}
	}

	private void recordReceiverStats(IterationEndsEvent event) {
		for (Receiver receiver : ReceiverUtils.getReceivers(sc).getReceivers().values()) {
			for (ReceiverOrder rorder : receiver.getSelectedPlan().getReceiverOrders()) {
				for (Order order : rorder.getReceiverProductOrders()) {
					String score = receiver.getSelectedPlan().getScore().toString();
					float start = (float) receiver.getSelectedPlan().getTimeWindows().get(0).getStart();
					float end = (float) receiver.getSelectedPlan().getTimeWindows().get(0).getEnd();
					float duration = (end - start) / 3600;
					float size = (float) (order.getDailyOrderQuantity() * order.getProduct().getProductType().getRequiredCapacity());
					float freq = (float) order.getNumberOfWeeklyDeliveries();
					float dur = (float) order.getServiceDuration();
					boolean status = (boolean) receiver.getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS);
					boolean member = (boolean) receiver.getAttributes().getAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER);


					try (BufferedWriter bw1 = IOUtils.getAppendingBufferedWriter(sc.getConfig().controller().getOutputDirectory() + RECEIVER_STATS_CSV)) {
						bw1.write(String.format("%d,%s,%s,%f,%f,%f,%s,%f,%f,%f,%b,%b",
							event.getIteration(),
							receiver.getId(),
							score,
							start,
							end,
							duration,
							order.getId(),
							size,
							freq,
							dur,
							status,
							member));
						bw1.newLine();

					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot write receiver stats");
					}
				}
			}
		}
	}

}



