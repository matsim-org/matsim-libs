package playground.dziemke.analysis.modalShare;

import org.apache.log4j.Logger;

/**
 * @author gthunig on 23.03.2017.
 */
public class ModalShareDistanceBinContainer extends DistanceBinContainer {

    public static final Logger log = Logger.getLogger(ModalShareDistanceBinContainer.class);

    enum Mode {
        CAR, PT, PT_SLOW, PT_FAST, WALK, BIKE, OTHER
    }

    ModalShareDistanceBinContainer(int binSize) {
        super(binSize, 7);
    }

    void enterDistance(int distance, Mode mode) {
        int numberOfValue = getNumberOfValue(mode);
        super.enterDistance(distance, numberOfValue);
    }

    static int getModeValue(DistanceBin bin, Mode mode) {
        int numberOfValue = getNumberOfValue(mode);
        return bin.getValues()[numberOfValue];
    }

    private static int getNumberOfValue(Mode mode) {
        switch (mode) {
            case CAR:
                return 0;
            case PT:
                return 1;
            case PT_SLOW:
                return 2;
            case PT_FAST:
                return 3;
            case WALK:
                return 4;
            case BIKE:
                return 5;
            case OTHER:
                return 6;
            default:
                log.warn("Unknown mode. Mode is set to OTHER.");
                return 6;
        }
    }

}
