package consensus.election.timeout;

import server.ServerState;
import consensus.LeaderState;
import consensus.election.FastBullyAlgorithm;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class ViewMessageTimeout extends MessageTimeout {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!ServerState.getInstance().getViewMessageReceived() && !interrupted.get()
                && ServerState.getInstance().getOngoingElection()) {
            // if view messages were not received, stop the election
            FastBullyAlgorithm stopFBA = new FastBullyAlgorithm("stopViewTimeout");
            stopFBA.stopElection();
            if (LeaderState.getInstance().getLeaderID() == null) {
                // if there isn't a leader, send itself as the leader
                FastBullyAlgorithm coordinatorFBA = new FastBullyAlgorithm("coordinatorViewTimeout");
                new Thread(coordinatorFBA).start();
            }
            ServerState.getInstance().setViewMessageReceived(false);
        }
    }
}
