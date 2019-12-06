import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

public class ActorExplorer extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().
                match
    }
}
