package playground.vsp.pipeline;

public interface ScenarioMultiSource {
	
	public ScenarioSource getSource(int index);
	
	public int getSourceCount();

}
