package consensus.election.timeout;

import server.ServerState;
import consensus.election.FastBullyAlgorithm;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class UpdateCompleteTimeout extends MessageTimeout {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!interrupted.get() && !ServerState.getInstance().getLeaderUpdateComplete()) {
            // restart the election procedure
            FastBullyAlgorithm.initialize();
        }
    }
}
