package playground.sebhoerl.agentlock.agent;

/**
 * The basic neede functionality for a LockAgent can either be implemented
 * by deriving from this class or delegation to LockAgentPlugin.
 */
abstract public class AbstractLockAgent extends LockAgentPlugin implements LockAgent {}
