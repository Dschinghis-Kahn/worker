package net.dschinghiskahn.worker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("PMD")
public class WorkerTest {

    private final Queue<String> queue = new LinkedList<String>();
    private final List<AbstractWorker<String>> workers = new ArrayList<AbstractWorker<String>>();
    private boolean result;
    private List<String> allItems;
    private Long timeout;

    @BeforeClass
    public static void init() {
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getRootLogger().removeAllAppenders();
        ConsoleAppender appender = new ConsoleAppender();
        appender.setLayout(new PatternLayout("%d %-5p: %m%n"));
        appender.activateOptions();
        Logger.getRootLogger().addAppender(appender);
    }

    @Before
    public void start() {
        queue.clear();
        workers.clear();
        result = true;
        allItems = new ArrayList<String>();
        timeout = null;

    }

    @After
    public void stop() {
        for (AbstractWorker<String> worker : workers) {
            worker.stop();
        }
    }

    @Test(timeout = 1000)
    public void canStop() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: canStop()");
        small();
        for (AbstractWorker<String> worker : workers) {
            worker.stop();
            Assert.assertTrue(!worker.getThread().isAlive());
        }
    }

    @Test(timeout = 1000)
    public void small() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: small()");
        testRun(10, 1);
    }

    @Test(timeout = 1000)
    public void large() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: large()");
        testRun(1000, 1);
    }

    @Test(timeout = 1000)
    public void smallMultiThread() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: smallMultiThread()");
        testRun(10, 10);
    }

    @Test(timeout = 1000)
    public void largeMultiThread() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: largeMultiThread()");
        testRun(1000, 10);
    }

    @Test(timeout = 1000)
    public void canWait() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: canWait()");
        TestWorker worker = new TestWorker();
        workers.add(worker);
        worker.start();

        Thread.sleep(100);

        queue.add("1");
        allItems.add("1");
        worker.wakeUpAllWorkers();

        while (!queue.isEmpty()) {
            Thread.sleep(100);
        }
    }

    private void testRun(int itemCount, int threadCount) throws InterruptedException {
        for (int i = 0; i < itemCount; i++) {
            queue.add(String.valueOf(i));
            allItems.add(String.valueOf(i));
        }

        for (int i = 0; i < threadCount; i++) {
            TestWorker worker = new TestWorker();
            workers.add(worker);
            worker.start();
        }

        while (!queue.isEmpty()) {
            Thread.sleep(100);
        }

        for (AbstractWorker<String> worker : workers) {
            while (!worker.isSuspended()) {
                Thread.sleep(100);
            }
        }

        Assert.assertTrue(result);
    }

    @Test(timeout = 1000)
    public void canWakeUp() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: canWakeUp()");
        queue.add("1");

        AbstractWorker<String> worker = new TestWorker();
        worker.start();

        while (!worker.isSuspended()) {
            Thread.sleep(100);
        }

        queue.add("2");

        worker.wakeUpAllWorkers();

        Thread.sleep(100);

        while (!worker.isSuspended()) {
            Thread.sleep(100);
        }

        Assert.assertTrue(queue.isEmpty());
    }

    @Test(timeout = 5000)
    public void suspendTime() throws InterruptedException {
        Logger.getLogger(WorkerTest.class).info("Running test: suspendTime()");
        timeout = 1000L;

        TestWorker worker = new TestWorker();
        worker.start();

        while (!worker.isSuspended()) {
            Thread.sleep(10);
        }

        long startTime = System.currentTimeMillis();

        queue.add("0");

        while (!queue.isEmpty()) {
            Thread.sleep(10);
        }

        long duration = System.currentTimeMillis() - startTime;
        Assert.assertTrue(duration > 900);
        Assert.assertTrue(queue.isEmpty());
    }

    private class TestWorker extends AbstractWorker<String> {

        TestWorker() {
            super("TestWorker");
        }

        @Override
        protected void doWork(String item) {
            synchronized (allItems) {
                if (!allItems.contains(item)) {
                    result = false;
                }
                allItems.remove(item);
            }
        }

        @Override
        protected String getWork() {
            return queue.poll();
        }

        @Override
        protected boolean isWorkAvailable() {
            return !queue.isEmpty();
        }

        @Override
        protected Long getSuspendTime() {
            return timeout;
        }
    }
}
