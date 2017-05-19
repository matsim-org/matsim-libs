package playground.fseccamo.dispatcher;

import java.util.Comparator;

public enum MpcRequestComparator implements Comparator<MpcRequest> {
    INSTANCE;
    // ---

    @Override
    public int compare(MpcRequest o1, MpcRequest o2) {
        return Double.compare( //
                o1.avRequest.getSubmissionTime(), //
                o2.avRequest.getSubmissionTime());
    }

}
