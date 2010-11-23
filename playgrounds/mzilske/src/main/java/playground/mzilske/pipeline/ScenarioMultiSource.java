package playground.mzilske.pipeline;

public interface ScenarioMultiSource {
	
	public ScenarioSource getSource(int index);
	
	public int getSourceCount();

}
