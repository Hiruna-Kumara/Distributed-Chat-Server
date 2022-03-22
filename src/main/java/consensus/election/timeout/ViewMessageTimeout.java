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
public class ViewMessageTimeout extends MessageTimeout {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if(!Server.getInstance().getViewMessageReceived() && !interrupted.get() && Server.getInstance().getOngoingElection()){
            //if view messages were not received, stop the election
            FastBullyAlgorithm stopFBA = new FastBullyAlgorithm("stopViewTimeout");
            stopFBA.stopElection();
            if(Leader.getInstance().getLeaderID() == null){
                //if there isn't a leader, send itself as the leader
                FastBullyAlgorithm coordinatorFBA = new FastBullyAlgorithm("coordinatorViewTimeout");
                new Thread(coordinatorFBA).start();
            }
            Server.getInstance().setViewMessageReceived(false);
        }
    }
}
