/**
 * 
 */
package playground.clruch.dispatcher.core;

import java.util.List;

/** @author Claudio Ruch */
public class InfoLine {
    private int infoLinePeriod = 0;

    InfoLine(int infoLinePeriod) {
        this.infoLinePeriod = infoLinePeriod;

    }

    /** derived classes should override this function to add details
     * 
     * @return */
    /* package */ String getInfoLine(List<RoboTaxi> robotaxis, double timeNow) {
        final String string = getClass().getSimpleName() + "        ";
        return String.format("%s@%6d V=(%4ds,%4dd)", //
                string.substring(0, 6), //
                (long) timeNow, //
                robotaxis.stream().filter(rt -> rt.isInStayTask()).count(), //
                robotaxis.stream().filter(rt -> (rt.getAVStatus().equals(AVStatus.DRIVETOCUSTMER) || rt.getAVStatus().equals(AVStatus.DRIVETOCUSTMER)))
                        .count());
    }

    /** @param infoLinePeriod
     *            positive values determine the period, negative values or 0 will disable the
     *            printout */
    public final void setInfoLinePeriod(int infoLinePeriod) {
        this.infoLinePeriod = infoLinePeriod;
    }

}
