import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.server.AllDirectives;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import javafx.util.Pair;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;


import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import static config.Config.*;
import static org.asynchttpclient.Dsl.asyncHttpClient;

public class Server  extends AllDirectives {

    public static void main(String[] args) throws IOException {
        System.out.println(ON_START);
        ActorSystem system = ActorSystem.create(SYSTEM_NAME);
        ActorRef explorer = system.actorOf(Props.create(ActorExplorer.class));

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(explorer, materializer);//вызов метода которому передаем Http, ActorSystem и ActorMaterializer;
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(HOST, PORT),
                materializer
        );
        System.out.println(SERVER_START_MESSAGE);
        System.in.read();
        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(ActorRef explorer, ActorMaterializer materializer) {

        return Flow.of(HttpRequest.class)
                .map(msg -> {
                    Query q = msg.getUri().query();
                    String url;
                    int count;
                    //if(q.get(ULR_PARAMETER).isPresent() & q.get(COUNT_PARAMETER).isPresent()){
                        url = q.get(ULR_PARAMETER).get();
                        count = Integer.parseInt(q.get(COUNT_PARAMETER).get());
                        return new Pair<>(url, count);
                   // }
                   // return Supervision.stop();
                }).mapAsync(MAX_STREAMS,  msg ->
                        Patterns.ask(explorer, new FindMessage(msg.getKey()), TIMEOUT)
                                .thenCompose(answer ->
                                        answer.getClass() == TestMessage.class ?
                                                CompletableFuture.completedFuture(answer)
                                                : takeSource(msg, materializer)))
                        .map(answer -> {
                            explorer.tell(answer, ActorRef.noSender());
                            return HttpResponse
                                .create()
                                .withStatus(StatusCodes.OK)
                                .withEntity(
                                    HttpEntities.create(
                                        answer.getUrl() + " " + answer.getCount()));
                        });
    }

    private static CompletionStage<Long> takeSource (Pair<String, Integer> pair, Materializer materializer) {
        return Source.from(Collections.singletonList(pair))
                .toMat(testSink(), Keep.right())
                .run(materializer)
                .thenApply(sum -> sum/pair.getValue());
    }

    private static Sink<Pair<String, Integer>, CompletionStage<Long>> testSink() {
        return Flow.<Pair<String, Integer>>create()
                .mapConcat(msg ->
                        Collections.nCopies(msg.getValue(), msg.getKey()))
                .mapAsync(MAX_STREAMS, url -> {
                    long zeroTime = System.nanoTime();
                    AsyncHttpClient client = asyncHttpClient();

                    return client
                            .prepareGet(url)
                            .execute()
                            .toCompletableFuture()
                            .exceptionally(throwable -> (Response) Supervision.stop())
                            .thenApply(response -> System.nanoTime() - zeroTime);
                }).toMat(Sink.fold(0L, Long::sum), Keep.right());
    }
}
