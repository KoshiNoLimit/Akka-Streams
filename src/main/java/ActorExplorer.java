import akka.actor.AbstractActor;
import akka.http.javadsl.model.ws.TextMessage;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

import static config.Config.HAVENT;

public class ActorExplorer extends AbstractActor {
    private Map<String, Long> store = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(FindMessage.class, msg -> {
                    if(!store.containsKey(msg.getUrl())) {
                        sender().tell(HAVENT,self());
                    } else {
                        sender().tell()
                    }

                })
                .match(TestMessage.class, msg -> {

                }).
        build();
    }
}
