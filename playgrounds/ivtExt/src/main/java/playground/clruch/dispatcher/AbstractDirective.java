package playground.clruch.dispatcher;

import playground.clruch.router.FuturePathContainer;

/**
 * class maintains a {@link FuturePathContainer}
 * while the path is being computer.
 * the resulting path is available upon the function call execute(...)
 */
abstract class AbstractDirective { // TODO rename with "Postponed/Delayed" + "Path"
    protected final FuturePathContainer futurePathContainer;

    AbstractDirective(FuturePathContainer futurePathContainer) {
        this.futurePathContainer = futurePathContainer;
    }
    
    abstract void execute();
}
