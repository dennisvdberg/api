package org.polkadot.example.rx;

import com.google.common.collect.Lists;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function4;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.polkadot.api.rx.ApiRx;
import org.polkadot.rpc.provider.ws.WsProvider;
import org.polkadot.types.codec.CodecUtils;

import java.util.List;
import java.util.stream.Collectors;

public class E05_ReadStorage {
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
        initEndPoint(args);

        WsProvider wsProvider = new WsProvider(endPoint);

        Observable<ApiRx> apiRxObservable = ApiRx.create(wsProvider);

        apiRxObservable.flatMap((apiRx) -> {

            return (Observable<Pair<ApiRx, Object>>) Observable.combineLatest(
                    Observable.just(apiRx),
                    apiRx.query().section("session").function("validators").call(),
                    new BiFunction<ApiRx, Object, Pair<ApiRx, Object>>() {
                        @Override
                        public Pair<ApiRx, Object> apply(ApiRx apiRx, Object o) throws Exception {
                            System.out.println("BiFunction  ");
                            return Pair.of(apiRx, CodecUtils.arrayLikeToList(o).get(0));
                        }
                    }
            );
        }).switchMap((result) -> {
            Pair<ApiRx, Object> pair = result;
            ApiRx apiRx = pair.getLeft();
            Object param = pair.getRight();

            List<Object> validators = CodecUtils.arrayLikeToList(param);
            Observable balances = null;
            if (!validators.isEmpty()) {

                List<Observable> collect = validators.stream().map(
                        authorityId -> {
                            return apiRx.query().section("balances").function("freeBalance").call(authorityId);
                        }

                ).collect(Collectors.toList());

                balances = Observable.combineLatest(
                        collect.toArray(new Observable[0]),
                        new Function<Object[], List>() {
                            @Override
                            public List apply(Object[] objects) throws Exception {
                                return Lists.newArrayList(objects);
                            }
                        }
                );
            } else {
                balances = Observable.just("null");
            }

            return (Observable<List>) Observable.combineLatest(
                    apiRx.query().section("system").function("accountNonce").call(Alice),
                    apiRx.query().section("timestamp").function("blockPeriod").call(),
                    Observable.just(validators),
                    balances,

                    (Function4<Object, Object, Object, Object, List>) (o, o2, objects1, o3) -> Lists.newArrayList(o, o2, objects1, o3)

            );

        }).subscribe((result) -> {

            System.out.println("accountNonce(" + Alice + ") " + result.get(0));

            List<Object> validators = CodecUtils.arrayLikeToList(result.get(2));
            if (CollectionUtils.isNotEmpty(validators)) {
                List<Object> balances = CodecUtils.arrayLikeToList(result.get(3));

                System.out.println("validators");
                for (int i = 0; i < validators.size(); i++) {
                    System.out.println(validators.get(i) + " : " + balances.get(i));
                }
            }
        });

    }
}
