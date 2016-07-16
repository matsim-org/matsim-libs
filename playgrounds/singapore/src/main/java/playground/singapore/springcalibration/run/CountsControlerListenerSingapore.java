package playground.singapore.springcalibration.run;

import org.apache.log4j.Logger;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsHtmlAndGraphsWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author anhorni
 */
class CountsControlerListenerSingapore implements StartupListener, IterationEndsListener {

	/*
	 * String used to identify the operation in the IterationStopWatch.
	 */
	private final static Logger log = Logger.getLogger(CountsControlerListenerSingapore.class);
	public static final String OPERATION_COMPARECOUNTS = "compare with counts";

    private GlobalConfigGroup globalConfigGroup;
    private Network network;
    private ControlerConfigGroup controlerConfigGroup;
    private CountsConfigGroup config;
    private Set<String> analyzedModes;
    private VolumesAnalyzer volumesAnalyzer;
    private IterationStopWatch iterationStopwatch;
    private OutputDirectoryHierarchy controlerIO;

    private Counts<Link> counts = null;

    private final Map<Id<Link>, double[]> linkStats = new HashMap<>();
    private int iterationsUsed = 0;

	@Override
	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		log.info("Starting up");
		Scenario scenario = controlerStartupEvent.getServices().getScenario();
		Config config = controlerStartupEvent.getServices().getConfig();
		
		this.globalConfigGroup = config.global();
		this.network = scenario.getNetwork();
		this.controlerConfigGroup = config.controler();
		this.config = config.counts();
		this.volumesAnalyzer = controlerStartupEvent.getServices().getVolumes();
		this.analyzedModes = CollectionUtils.stringToSet(this.config.getAnalyzedModes());
        this.iterationStopwatch = controlerStartupEvent.getServices().getStopwatch();
        this.controlerIO = controlerStartupEvent.getServices().getControlerIO();
        
        this.counts = (Counts<Link>) scenario.getScenarioElement(Counts.ELEMENT_NAME);
				
        if (counts != null) {
            for (Id<Link> linkId : counts.getCounts().keySet()) {
                this.linkStats.put(linkId, new double[24]);
            }
        }
	}

    @Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
    	log.info("Iteration ends call");
		if (counts != null && this.config.getWriteCountsInterval() > 0) {			
            if (useVolumesOfIteration(event.getIteration(), controlerConfigGroup.getFirstIteration())) {
                addVolumes(volumesAnalyzer);
            }
            if (createCountsInIteration(event.getIteration())) {          	
                iterationStopwatch.beginOperation(OPERATION_COMPARECOUNTS);
                Map<Id<Link>, double[]> averages;
                if (this.iterationsUsed > 1) {
                    averages = new HashMap<>();
                    for (Map.Entry<Id<Link>, double[]> e : this.linkStats.entrySet()) {
                        Id<Link> linkId = e.getKey();
                        double[] totalVolumesPerHour = e.getValue();
                        double[] averageVolumesPerHour = new double[totalVolumesPerHour.length];
                        for (int i = 0; i < totalVolumesPerHour.length; i++) {
                            averageVolumesPerHour[i] = totalVolumesPerHour[i] / this.iterationsUsed;
                        }
                        averages.put(linkId, averageVolumesPerHour);
                    }
                } else {
                    averages = this.linkStats;
                }
                CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(averages, counts, network, config.getCountsScaleFactor());
                if ((this.config.getDistanceFilter() != null) && (this.config.getDistanceFilterCenterNode() != null)) {
                    cca.setDistanceFilter(this.config.getDistanceFilter(), this.config.getDistanceFilterCenterNode());
                }
                cca.setCountsScaleFactor(this.config.getCountsScaleFactor());
                cca.run();
                
                if (this.config.getOutputFormat().contains("html") ||
                        this.config.getOutputFormat().contains("all")) {                	
                	String path = controlerIO.getIterationPath(event.getIteration()) + "/screenline/";
                	new File(path).mkdir();
                	log.info("Writing counts to: " + path);
                    CountsHtmlAndGraphsWriter cgw = new CountsHtmlAndGraphsWriter(path, cca.getComparison(), event.getIteration());
                    cgw.addGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
                    cgw.addGraphsCreator(new CountsErrorGraphCreator("errors"));
                    cgw.addGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
                    cgw.addGraphsCreator(new CountsSimReal24GraphCreator("average working day sim and count volumes"));
                    cgw.createHtmlAndGraphs();
                }
                if (this.config.getOutputFormat().contains("kml") ||
                        this.config.getOutputFormat().contains("all")) {
                    String filename = controlerIO.getIterationFilename(event.getIteration(), "countscompare_screenline.kmz");
                    CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
                            cca.getComparison(), network, TransformationFactory.getCoordinateTransformation(globalConfigGroup.getCoordinateSystem(), TransformationFactory.WGS84));
                    kmlWriter.setIterationNumber(event.getIteration());
                    kmlWriter.writeFile(filename);
                }
                if (this.config.getOutputFormat().contains("txt") ||
                        this.config.getOutputFormat().contains("all")) {
                    String filename = controlerIO.getIterationFilename(event.getIteration(), "countscompare_screenline.txt");
                    CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(cca.getComparison(), Locale.ENGLISH);
                    ctw.writeFile(filename);
                }
                reset();
                iterationStopwatch.endOperation(OPERATION_COMPARECOUNTS);
            }
        }
	}

	/*package*/ boolean useVolumesOfIteration(final int iteration, final int firstIteration) {
		int iterationMod = iteration % this.config.getWriteCountsInterval();
		int effectiveIteration = iteration - firstIteration;
		int averaging = Math.min(this.config.getAverageCountsOverIterations(), this.config.getWriteCountsInterval());
		if (iterationMod == 0) {
			return ((this.config.getAverageCountsOverIterations() <= 1) ||
					(effectiveIteration >= averaging));
		}
		return (iterationMod > (this.config.getWriteCountsInterval() - this.config.getAverageCountsOverIterations())
				&& (effectiveIteration + (this.config.getWriteCountsInterval() - iterationMod) >= averaging));
	}
	
	/*package*/ boolean createCountsInIteration(final int iteration) {
		return ((iteration % this.config.getWriteCountsInterval() == 0) && (this.iterationsUsed >= this.config.getAverageCountsOverIterations()));		
	}

	private void addVolumes(final VolumesAnalyzer volumes) {
		this.iterationsUsed++;
		for (Map.Entry<Id<Link>, double[]> e : this.linkStats.entrySet()) {
			Id<Link> linkId = e.getKey();
			double[] volumesPerHour = e.getValue(); 
			double[] newVolume = getVolumesPerHourForLink(volumes, linkId); 
			for (int i = 0; i < 24; i++) {
				volumesPerHour[i] += newVolume[i];
			}
		}
	}
	
	private double[] getVolumesPerHourForLink(final VolumesAnalyzer volumes, final Id<Link> linkId) {
		if (this.config.isFilterModes()) {
			double[] newVolume = new double[24];
			for (String mode : this.analyzedModes) {
				double[] volumesForMode = volumes.getVolumesPerHourForLink(linkId, mode);
				if (volumesForMode == null) continue;
				
				// NEW: we have to rescale the pt volumes here as they will be upscaled latter! ---------------
				if (mode.equals("pt") || mode.equals("bus")) {
					volumesForMode = this.scaleVolumes(volumesForMode);
				}	
				// --------------------------------------------------------------------------------------------
				
				for (int i = 0; i < 24; i++) {
					newVolume[i] += volumesForMode[i];
				}				
				Id<Link> linkIdHW = Id.createLinkId(linkId.toString() + "_HW");
				double[] volumesForMode_HW = volumes.getVolumesPerHourForLink(linkIdHW, mode);
				if (volumesForMode_HW == null) continue;
				
				if (mode.equals("pt") || mode.equals("bus")) {
					volumesForMode_HW = this.scaleVolumes(volumesForMode_HW);
				}
				
				for (int i = 0; i < 24; i++) {
					newVolume[i] += volumesForMode_HW[i];
				}
				
			}
			return newVolume;
		} else {
			double[] newVolume = new double[24];
			Id<Link> linkIdHW = Id.createLinkId(linkId.toString() + "_HW");
						
			for (int i = 0; i < 24; i++) {
				newVolume[i] += volumes.getVolumesPerHourForLink(linkId)[i] + volumes.getVolumesPerHourForLink(linkIdHW)[i];
			}
			return newVolume;
		}
	}
	
	private double[] scaleVolumes(double[] volumes) {
		for (int i = 0; i < 24; i++) {
			volumes[i] = volumes[i] / this.config.getCountsScaleFactor();
		}
		return volumes;
	}
	
	private void reset() {
		this.iterationsUsed = 0;
		for (double[] hours : this.linkStats.values()) {
			for (int i = 0; i < hours.length; i++) {
				hours[i] = 0.0;
			}
		}
	}

}
