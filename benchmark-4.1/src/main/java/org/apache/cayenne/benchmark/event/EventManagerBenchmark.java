package org.apache.cayenne.benchmark.event;

import java.util.concurrent.TimeUnit;

import org.apache.cayenne.access.event.SnapshotEvent;
import org.apache.cayenne.benchmark.event.utils.FakeListener;
import org.apache.cayenne.benchmark.event.utils.FakeSender;
import org.apache.cayenne.event.CayenneEvent;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.EventSubject;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 4, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@State(Scope.Benchmark)
public class EventManagerBenchmark {

    private EventManager eventManager;
    private FakeListener fakeListener;
    private FakeSender fakeSender;
    private EventSubject eventSubject;

    @Setup(Level.Trial)
    public void setUp() {
        this.eventManager = new DefaultEventManager();
        this.fakeListener = new FakeListener();
        this.fakeSender = new FakeSender();
        this.eventSubject = EventSubject.getSubject(fakeSender.getClass(), "test");
        eventManager.addNonBlockingListener(fakeListener,
                "testMethod",
                SnapshotEvent.class,
                eventSubject,
                fakeSender);
    }

    @Benchmark
    @Threads(3)
    public void addNonBlockingListener() {
        eventManager.addNonBlockingListener(new FakeListener(),
                "testMethod",
                SnapshotEvent.class,
                eventSubject,
                fakeSender);
    }

    @Benchmark
    @Threads(3)
    public boolean removeListener() {
        return eventManager.removeListener(fakeListener);
    }

    @Benchmark
    @Threads(3)
    public void postEvent() {
        eventManager.postEvent(new CayenneEvent(fakeSender), eventSubject);
    }
}
