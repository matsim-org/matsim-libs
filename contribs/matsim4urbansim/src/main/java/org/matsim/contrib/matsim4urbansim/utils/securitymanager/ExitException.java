package org.matsim.contrib.matsim4urbansim.utils.securitymanager;

public class ExitException extends SecurityException {
    private static final long serialVersionUID = -1982617086752946683L;
    public final int status;

    public ExitException(int status) {
        super("System.exit() detected!!!");
        this.status = status;
    }
}
