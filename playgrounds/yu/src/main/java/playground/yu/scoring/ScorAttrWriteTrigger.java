/**
 * 
 */
package playground.yu.scoring;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * decides when to write scoring function attributes into a ASCII file
 * 
 * @author yu
 * 
 */
public class ScorAttrWriteTrigger implements IterationEndsListener {

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();
		Controler4AttrRecorder controler = (Controler4AttrRecorder) event
		.getControler();
		String writeScorAttrIntervalStr = controler.getConfig().getParam("bse",
		"writeScorAttrInterval");
		if (writeScorAttrIntervalStr != null) {
			int writeScorAttrInterval = Integer
			.parseInt(writeScorAttrIntervalStr);
			if (writeScorAttrInterval > 0
					&& iteration % writeScorAttrInterval == 0) {
				controler
				.getPlanScoring4AttrRecorder()
				.getPlanScorer()
				.writeScoreAttrs(
						controler.getControlerIO()
						.getIterationFilename(iteration,
								"scorAttr.log.gz"));
			}
		}
	}

}
