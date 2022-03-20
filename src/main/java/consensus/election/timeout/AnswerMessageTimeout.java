package consensus.election.timeout;

import server.ServerState;
import consensus.election.FastBullyAlgorithm;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@DisallowConcurrentExecution
public class AnswerMessageTimeout extends MessageTimeout {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (ServerState.getInstance().getOngoingElection()) {
            if (ServerState.getInstance().getAnswerMessageReceived() || interrupted.get()) {
                // answer messages received, send the nominator message
                FastBullyAlgorithm nominationFBA = new FastBullyAlgorithm("sendNominationAnswerTimeout");
                new Thread(nominationFBA).start();
                // nominationFBA.sendNominationMessage("sendNominationAnswerTimeout");

            } else {
                // answer messages were not received send coordinator message to lower priority
                // servers
                FastBullyAlgorithm coordinatorFBA = new FastBullyAlgorithm("coordinatorAnswerTimeout");
                new Thread(coordinatorFBA).start();
            }
        }

    }
}
