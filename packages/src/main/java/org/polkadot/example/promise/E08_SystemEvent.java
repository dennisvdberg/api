package org.polkadot.example.promise;

import com.onehilltech.promises.Promise;
import org.polkadot.api.Types;
import org.polkadot.api.promise.ApiPromise;
import org.polkadot.direct.IRpcFunction;
import org.polkadot.rpc.provider.ws.WsProvider;
import org.polkadot.types.Codec;
import org.polkadot.types.codec.CreateType;
import org.polkadot.types.codec.Vector;
import org.polkadot.types.type.Event;
import org.polkadot.types.type.EventRecord;

import java.util.List;

public class E08_SystemEvent {
    static String Alice = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY";

    //static String endPoint = "wss://poc3-rpc.polkadot.io/";
    //static String endPoint = "wss://substrate-rpc.parity.io/";
    //static String endPoint = "ws://45.76.157.229:9944/";
    static String endPoint = "ws://127.0.0.1:9944";

    static void initEndPoint(String[] args) {
        if (args != null && args.length >= 1) {
            endPoint = args[0];
            System.out.println(" connect to endpoint [" + endPoint + "]");
        } else {
            System.out.println(" connect to default endpoint [" + endPoint + "]");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Create an await for the API
        //Promise<ApiPromise> ready = ApiPromise.create();
        initEndPoint(args);

        WsProvider wsProvider = new WsProvider(endPoint);

        Promise<ApiPromise> ready = ApiPromise.create(wsProvider);

        ready.then(api -> {
                    Types.QueryableStorage<Promise> query = api.query();
                    Types.QueryableModuleStorage<Promise> system = query.section("system");
                    return Promise.all(
                            system.function("events").call((IRpcFunction.SubscribeCallback) (events) -> {
                                //List list = (List) events;
                                //Vector<EventRecord> eventRecords = ((Vector<EventRecord>) list.get(0));

                                Vector<EventRecord> eventRecords = (Vector<EventRecord>) events;
                                System.out.println("events  " + eventRecords.size());

                                // loop through the Vec<EventRecord>
                                for (EventRecord eventRecord : eventRecords) {
                                    EventRecord.Phase phase = eventRecord.getPhase();
                                    Event event = eventRecord.getEvent();

                                    List<CreateType.TypeDef> types = event.getTypeDef();

                                    // show what we are busy with
                                    System.out.println("\t" + event.getSection() + ":" + event.getMethod() + ":: (phase=" + phase.toString() + ")");
                                    System.out.println("\t\t" + event.getMeta().getDocumentation().toString());

                                    // loop through each of the parameters, displaying the type and data
                                    for (int index = 0; index < event.getData().size(); index++) {
                                        Codec data = event.getData().get(index);
                                        System.out.println("\t\t\t" + types.get(index).getType() + ": " + data.toString());
                                    }
                                }
                            })
                    );
                }
        ).then(
                (result) -> {
                    System.out.println(result);
                    return null;
                }
        )._catch((err) -> {
            err.printStackTrace();
            return Promise.value(err);
        });

    }
}
