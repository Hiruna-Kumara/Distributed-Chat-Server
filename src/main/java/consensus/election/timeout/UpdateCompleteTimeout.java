package consensus.election.timeout;

import Server.Server;
import Server.ServerInfo;
import consensus.Leader;
import consensus.election.FastBullyAlgorithm;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class UpdateCompleteTimeout extends MessageTimeout {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!interrupted.get() && !Server.getInstance().getLeaderUpdateComplete()) {
            // restart the election procedure
            FastBullyAlgorithm.initialize();
        }
    }
}
