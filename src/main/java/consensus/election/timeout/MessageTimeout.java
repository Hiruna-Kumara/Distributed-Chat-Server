package consensus.election.timeout;

import Server.Server;
import org.quartz.*;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MessageTimeout implements Job, InterruptableJob{

    protected Server server = Server.getInstance();
    protected AtomicBoolean interrupted = new AtomicBoolean(false);

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        interrupted.set(true);
        System.out.println("Job was interrupted...");
    }

}
