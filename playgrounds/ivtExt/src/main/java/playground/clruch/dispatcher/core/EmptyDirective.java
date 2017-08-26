// code by jph
package playground.clruch.dispatcher.core;

/**
 * {@link EmptyDirective} is assigned to a vehicle that already
 * is in the desired state but should not be available to
 * be assigned yet another Directive within the iteration.
 */
enum EmptyDirective implements AbstractDirective {
    INSTANCE
    ;

    @Override
    public void execute() {
        // intentionally blank
    }

}
