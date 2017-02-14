package playground.clruch.dispatcher;

import playground.clruch.router.FuturePathContainer;

abstract class AbstractDirective { // TODO rename with "Postponed/Delayed"
    protected final FuturePathContainer futurePathContainer;

    AbstractDirective(FuturePathContainer futurePathContainer) {
        this.futurePathContainer = futurePathContainer;
    }
    
    abstract void execute(final double getTimeNow);
}
