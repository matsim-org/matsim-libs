package playground.fseccamo.dispatcher;

import java.util.Comparator;

import playground.sebhoerl.avtaxi.passenger.AVRequest;

public enum AVRequestComparator implements Comparator<AVRequest> {
    INSTANCE;
    // ---

    @Override
    public int compare(AVRequest avr1, AVRequest avr2) {
        return Double.compare(avr1.getSubmissionTime(), //
                avr2.getSubmissionTime());
    }

}
