package consensus.election.timeout;

import server.Server;
import server.ServerState;
import consensus.election.FastBullyAlgorithm;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class CoordinatorMessageTimeout extends MessageTimeout {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!interrupted.get() && ServerState.getInstance().getOngoingElection()) {
            try {
                // if no coordinator message received, send the nomination to the next highest
                // server
                Server highestPriorityCandidate = ServerState.getInstance().getHighestPriorityCandidate();
                FastBullyAlgorithm nominationFBA;
                if (highestPriorityCandidate != null) {
                    nominationFBA = new FastBullyAlgorithm("sendNominationCoordinatorTimeout", jobExecutionContext);
                } else {
                    nominationFBA = new FastBullyAlgorithm("restart_election");
                }
                new Thread(nominationFBA).start();
            } catch (NullPointerException ne) {
                System.out.println("highestPriorityCandidate is null");
            }
        }
    }
}
