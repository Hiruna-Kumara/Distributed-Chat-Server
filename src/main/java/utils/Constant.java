package utils;

public abstract class Constant {

    private Constant() {
    } // no init

    public static final String ALIVE_JOB = "AliveJob".toUpperCase();
    public static final String ALIVE_JOB_TRIGGER = "AliveJobTrigger".toUpperCase();
    public static final String CONSENSUS_JOB = "ConsensusJob".toUpperCase();
    public static final String CONSENSUS_JOB_TRIGGER = "ConsensusJobTrigger".toUpperCase();
    public static final String GOSSIP_JOB = "GossipJob".toUpperCase();
    public static final String GOSSIP_JOB_TRIGGER = "GossipJobTrigger".toUpperCase();
    public static final String ELECTION_TRIGGER = "election_trigger".toUpperCase();

}