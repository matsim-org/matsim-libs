package playground.anhorni.choiceSetGeneration.io;

import java.util.List;
import org.apache.log4j.Logger;
import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;

public abstract class CSWriter {
		
	private final static Logger log = Logger.getLogger(CSWriter.class);
	
	public abstract void write(String outdir, String name, List<ChoiceSet> choiceSets);
	
	public CSWriter() {
	}
	
	protected boolean checkBeforeWriting(List<ChoiceSet> choiceSets) {
		if (choiceSets == null) {
			log.error("No choice set defined");
			return false;
		}
		if (choiceSets.size() == 0) {
			log.info("Empty choice set");
			return false;
		}		
		return true;
	}
}
