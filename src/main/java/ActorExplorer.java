import akka.actor.AbstractActor;
import akka.http.javadsl.model.ws.TextMessage;
import akka.japi.pf.ReceiveBuilder;

public class ActorExplorer extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(FindMessage.class, msg -> {

                })
                .match(TestMessage.class, msg -> {

                }).
        build();
    }
}
