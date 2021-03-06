package org.apache.cayenne.stable.benchmark.server;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.access.translator.select.SelectTranslator;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.stable.benchmark.event.utils.NoopEventManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import persistent.Artist;

@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 6, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@State(Scope.Benchmark)
public class ReadComponentsBenchmark {

    private static ServerRuntime serverRuntime;

    @Setup(Level.Iteration)
    public void setUp() {
        serverRuntime = ServerRuntime.builder()
                .addConfig("cayenne-project.xml")
                .addModule(binder -> ServerModule.contributeProperties(binder)
                        .put(Constants.SERVER_CONTEXTS_SYNC_PROPERTY, String.valueOf(false)))
                .addModule(binder -> binder.bind(EventManager.class).toInstance(new NoopEventManager()))
                .build();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        serverRuntime.shutdown();
    }

    @State(Scope.Benchmark)
    public static class QuerySetup {

        ObjectContext objectContext;
        //TODO for 4.2 move back to ObjsectSelect
        SelectQuery<DataRow> query;
        DbAdapter dbAdapter;

        @Setup(Level.Invocation)
        public void setUp() {
            objectContext = serverRuntime.newContext();
            query = SelectQuery.dataRowQuery(Artist.class);
            Collection<DataNode> dataNodes = ((DataDomain)objectContext.getChannel()).getDataNodes();
            dbAdapter = dataNodes.iterator().next().getAdapter();
        }
    }

    @State(Scope.Benchmark)
    public static class ContextSetup {

        ObjectContext objectContext;

        @Setup(Level.Invocation)
        public void setUp() {
            objectContext = serverRuntime.newContext();
        }
    }

    @Benchmark
    public ObjectContext contextCreation() {
        return serverRuntime.newContext();
    }

    @Benchmark
    public ObjectSelect<DataRow> queryCreation() {
        return ObjectSelect.dataRowQuery(Artist.class);
    }

    @Benchmark
    public String queryTranslation(QuerySetup querySetup) throws Exception {
        SelectTranslator selectTranslator = new DefaultSelectTranslator(querySetup.query, querySetup.dbAdapter, querySetup.objectContext.getEntityResolver());
        return selectTranslator.getSql();
    }

    @Benchmark
    public List<DataRow> fullQueryDataRowExecution(ContextSetup contextSetup) {
        return ObjectSelect.dataRowQuery(Artist.class)
                .select(contextSetup.objectContext);
    }

    @Benchmark
    public List<Artist> fullQueryEntityExecution(ContextSetup contextSetup) {
        return ObjectSelect.query(Artist.class)
                .select(contextSetup.objectContext);
    }

    @Benchmark
    public List<Artist> fullExecution() {
        return ObjectSelect.query(Artist.class)
                .select(serverRuntime.newContext());
    }
}
