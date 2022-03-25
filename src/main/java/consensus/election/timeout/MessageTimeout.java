package consensus.election.timeout;

import Server.Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MessageTimeout implements Job, InterruptableJob{

    private static final Logger LOG = LogManager.getLogger(MessageTimeout.class);

    protected Server server = Server.getInstance();
    protected AtomicBoolean interrupted = new AtomicBoolean(false);

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        interrupted.set(true);
        // System.out.println("Job was interrupted...");
        LOG.info("Job was interrupted...");
    }

}
