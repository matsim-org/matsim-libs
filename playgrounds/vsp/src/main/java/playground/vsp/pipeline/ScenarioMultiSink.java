package playground.vsp.pipeline;

public interface ScenarioMultiSink {

	public ScenarioSink getSink(int index);
	
	public int getSinkCount();
	
}
