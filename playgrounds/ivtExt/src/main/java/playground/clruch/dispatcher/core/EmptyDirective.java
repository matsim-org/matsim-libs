// code by jph
package playground.clruch.dispatcher.core;

/**
 * {@link EmptyDirective} is assigned to a vehicle that already
 * is in the desired state but should not be available to
 * be assigned yet another Directive within the iteration.
 */
class EmptyDirective extends AbstractDirective {

    @Override
    void execute() {
        // intentionally blank
    }

}
