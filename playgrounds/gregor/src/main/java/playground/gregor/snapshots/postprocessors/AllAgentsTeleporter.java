package playground.gregor.snapshots.postprocessors;

import playground.gregor.snapshots.writers.PositionInfo;

public class AllAgentsTeleporter extends EvacuationLinksTeleporter {
	
	@Override
	public String[] processEvent(String[] event) {
			event[11] = TELEPORTATION_X;
			event[12] = TELEPORTATION_Y;
			return event;
	}
	
	@Override
	public void processPositionInfo(PositionInfo pos) {
		
			pos.setEasting(D_TELEPORTATION_X);
			pos.setNorthing(D_TELEPORTATION_Y);
		
		
		
	}

}
