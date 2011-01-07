package playground.mzilske.pipeline;

public interface PersonMultiSink {
	
	PersonSink getSink(int index);
	
	int getSinkCount();

}
