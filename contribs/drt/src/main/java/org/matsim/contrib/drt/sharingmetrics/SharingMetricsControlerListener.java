package org.matsim.contrib.drt.sharingmetrics;

import com.google.inject.Inject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class SharingMetricsControlerListener implements IterationEndsListener {
    private final MatsimServices matsimServices;

    private final DrtConfigGroup drtConfigGroup;
    private final SharingMetricsTracker sharingFactorTracker;

    private boolean headerWritten = false;

    private final String runId;

    private final String delimiter;

    private static final String notAvailableString = "NA";


    @Inject
    public SharingMetricsControlerListener(Config config,
										   DrtConfigGroup drtConfigGroup,
										   SharingMetricsTracker sharingFactorTracker,
										   MatsimServices matsimServices) {
        this.drtConfigGroup = drtConfigGroup;
        this.sharingFactorTracker = sharingFactorTracker;
        this.matsimServices = matsimServices;
        runId = Optional.ofNullable(config.controller().getRunId()).orElse(notAvailableString);
        this.delimiter = config.global().getDefaultDelimiter();

    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
		int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && event.getIteration() % createGraphsInterval == 0;

        Map<Id<Request>, Double> sharingFactors = sharingFactorTracker.getSharingFactors();
        Map<Id<Request>, Boolean> poolingRates = sharingFactorTracker.getPoolingRates();

        writeAndPlotSharingMetrics(
                sharingFactors,
                poolingRates,
                filename(event, "sharingFactors", ".png"),
                filename(event, "poolingRates", ".png"),
                filename(event, "sharingMetrics", ".csv"),
                createGraphs);

        double nPooled = poolingRates.values().stream().filter(b -> b).count();
        double nTotal = poolingRates.values().size();
        double meanPoolingRate = nPooled / nTotal;
        double meanSharingFactor = sharingFactors.values().stream().mapToDouble(d -> d).average().orElse(Double.NaN);

        writeIterationPoolingStats(meanPoolingRate + delimiter + meanSharingFactor + delimiter + nPooled +delimiter + nTotal, event.getIteration());
    }

    private void writeAndPlotSharingMetrics(Map<Id<Request>, Double> sharingFactorByRequest,
                                             Map<Id<Request>, Boolean> rates,
                                             String sharingFactors,
                                             String poolingRates,
                                             String csvFile,
                                             boolean createGraphs) {
        try (var bw = IOUtils.getBufferedWriter(csvFile)) {
            bw.append(line("Request", "SharingFactor", "Pooled"));

            for (Map.Entry<Id<Request>, Double> sharingFactorEntry : sharingFactorByRequest.entrySet()) {
                bw.append(line(sharingFactorEntry.getKey(), sharingFactorEntry.getValue(),rates.get(sharingFactorEntry.getKey())));
            }
            bw.flush();

            if (createGraphs) {
                final DefaultBoxAndWhiskerCategoryDataset sharingFactorDataset
                        = new DefaultBoxAndWhiskerCategoryDataset();

                final DefaultBoxAndWhiskerCategoryDataset poolingRateDataset
                        = new DefaultBoxAndWhiskerCategoryDataset();

                sharingFactorDataset.add(sharingFactorByRequest.values().stream().toList(), "", "");
                poolingRateDataset.add(rates.values().stream().toList(), "", "");

                JFreeChart chartRides = ChartFactory.createBoxAndWhiskerChart("Sharing factor", "", "Factor", sharingFactorDataset, false);
                JFreeChart chartPooling = ChartFactory.createBoxAndWhiskerChart("Pooling rate", "", "Rate", poolingRateDataset, false);

                ((BoxAndWhiskerRenderer) chartRides.getCategoryPlot().getRenderer()).setMeanVisible(true);
                ((BoxAndWhiskerRenderer) chartPooling.getCategoryPlot().getRenderer()).setMeanVisible(true);
                ChartUtils.writeChartAsPNG(new FileOutputStream(sharingFactors), chartRides, 1500, 1500);
                ChartUtils.writeChartAsPNG(new FileOutputStream(poolingRates), chartPooling, 1500, 1500);
            }
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String filename(IterationEndsEvent event, String prefix, String extension) {
        return matsimServices.getControlerIO()
                .getIterationFilename(event.getIteration(), prefix + "_" + drtConfigGroup.getMode() + extension);
    }

    private String line(Object... cells) {
        return Arrays.stream(cells).map(Object::toString).collect(Collectors.joining(delimiter, "", "\n"));
    }


    private void writeIterationPoolingStats(String summarizePooling, int it) {
        try (var bw = getAppendingBufferedWriter("drt_sharing_metrics", ".csv")) {
            if (!headerWritten) {
                headerWritten = true;
                bw.write(line("runId", "iteration", "poolingRate", "sharingFactor", "nPooled", "nTotal"));
            }
            bw.write(runId + delimiter + it + delimiter + summarizePooling);
            bw.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
        return IOUtils.getAppendingBufferedWriter(matsimServices.getControlerIO().getOutputFilename(prefix + "_" + drtConfigGroup.getMode() + extension));
    }
}
