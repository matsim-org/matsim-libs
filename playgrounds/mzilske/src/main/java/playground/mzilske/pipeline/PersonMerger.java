package playground.mzilske.pipeline;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.Person;

public class PersonMerger implements PersonMultiSinkSource {

	private PersonSink sink;
	
	private int completeCount;

	private ArrayList<ProxySinkSource> sinks;

	public PersonMerger(int nSinks) {
		sinks = new ArrayList<ProxySinkSource>();
		for (int i = 0; i < nSinks; i++) {
			sinks.add(i, new ProxySinkSource());
		}
	}

	@Override
	public PersonSink getSink(int index) {
		return sinks.get(index);
	}

	@Override
	public int getSinkCount() {
		return sinks.size();
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}
	
	private void completeIfAllCompleted() {
		++completeCount;
		if (completeCount == sinks.size()) {
			sink.complete();
		}
	}

	private class ProxySinkSource implements PersonSink {

		public ProxySinkSource() {
			// Nothing to do.
		}

		@Override
		public void complete() {
			completeIfAllCompleted();
		}

		@Override
		public void process(Person person) {
			sink.process(person);
		}

	}
	
}
