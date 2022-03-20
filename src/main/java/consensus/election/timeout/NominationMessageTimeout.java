package consensus.election.timeout;

import server.ServerState;
import consensus.election.FastBullyAlgorithm;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class NominationMessageTimeout extends MessageTimeout {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (ServerState.getInstance().getOngoingElection()) {
            if (!interrupted.get()) {
                // If nomination message not received, restart the election procedure
                FastBullyAlgorithm startFBA = new FastBullyAlgorithm("restart_election");
                new Thread(startFBA).start();
            }
        }
    }
}
