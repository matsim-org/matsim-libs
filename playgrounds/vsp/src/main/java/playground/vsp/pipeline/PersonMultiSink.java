package playground.vsp.pipeline;

public interface PersonMultiSink {
	
	PersonSink getSink(int index);
	
	int getSinkCount();

}
