package net.dschinghiskahn.worker;

import java.lang.Thread.State;

import org.apache.log4j.Logger;

/**
 * This object provides several methods in order to implement an efficient
 * worker that e.g. processes items from a queue.
 * 
 * @param <E>
 *            The type of items that worker should handle.
 */
public abstract class AbstractWorker<E> implements Runnable {

    private final Logger logger = Logger.getLogger(getClass()); // NOPMD
    private Thread thread;
    private boolean running;
    private int workerId;
    private boolean suspended;
    private static int threadCount;
    private Object syncObject = getClass();

    /**
     * Creates a new worker in daemon mode.
     */
    public AbstractWorker(String name) {
        this(name, true);
    }

    /**
     * Creates a new worker
     * 
     * @param name
     *            Name of the worker
     * @param isDaemon
     *            Configure daemon mode
     */
    public AbstractWorker(String name, boolean isDaemon) {
        this(name, isDaemon, null);
    }

    /**
     * Creates a new worker
     * 
     * @param name
     *            Name of the worker
     * @param isDaemon
     *            Configure daemon mode
     * @param syncObject
     *            Use custom synchronization object or getClass()
     */
    public AbstractWorker(String name, boolean isDaemon, Object syncObject) {
        if (syncObject != null) {
            this.syncObject = syncObject;
        }
        synchronized (AbstractWorker.class) {
            this.workerId = AbstractWorker.threadCount++;
        }
        if (name == null) {
            thread = new Thread(this, "Worker" + workerId);
        } else {
            thread = new Thread(this, name + workerId);
        }
        getThread().setDaemon(isDaemon);
    }

    /**
     * Starts the worker thread
     */
    public void start() {
        if (!running && getThread().getState() == State.NEW) {
            getThread().start();
        }
    }

    /**
     * Stops the worker thread. The worker will stop after the next completed
     * execution of doWork.
     */
    public void stop() {
        if (running) {
            running = false;

            while (getThread().isAlive()) {
                if (suspended) {
                    wakeUpAllWorkers();
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    logger.error("Thread was interrupted", e);
                }
            }

            AbstractWorker.threadCount--;
        }
    }

    /**
     * Returns the id of the worker. The id is a sequence number that is
     * assigned during creation.
     * 
     * @return The id of the worker.
     */
    public int getId() {
        return workerId;
    }

    /**
     * Wakes up all workers from their suspended state. Useful to announce that
     * new work is available.
     */
    public void wakeUpAllWorkers() {
        synchronized (syncObject) {
            syncObject.notifyAll();
        }
    }

    /**
     * This method should never be called directly. Use the {@link #start()}
     * method instead.
     */
    @Override
    public void run() {
        running = true;
        suspended = false;
        Long time = null;
        E item = null;
        while (running) {
            synchronized (syncObject) {
                if (isWorkAvailable()) {
                    item = getWork();
                } else {
                    time = getSuspendTime();
                    if (logger.isDebugEnabled()) {
                        logger.debug(getThread().getName() + " - Suspending");
                    }
                    try {
                        suspended = true;
                        if (time == null) {
                            syncObject.wait();
                        } else {
                            syncObject.wait(time);
                        }
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug(getThread().getName() + " - Waking up");
                    }
                }
            }
            if (suspended) {
                suspended = false;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(getThread().getName() + " - Doing work");
                }
                doWork(item);
            }
        }
    }

    /**
     * This method is called by the worker as soon as {@link #isWorkAvailable()}
     * returns true.
     * 
     * @param item
     *            The item to do the work with.
     */
    protected abstract void doWork(E item);

    /**
     * This method is used to fetch the next item to work with. Important: This
     * method must not block!
     * 
     * @return The item to work with.
     */
    protected abstract E getWork();

    /**
     * Returns true if new work is available, false otherwise. Important: This
     * method must not block!
     * 
     * @return True if new work is available, false otherwise.
     */
    protected abstract boolean isWorkAvailable();

    /**
     * Returns the amount of time in milliseconds that a worker sleeps when
     * {@link #isWorkAvailable()} returns false. If null is returned the worker
     * waits until notified.
     * 
     * @return The amount of time in milliseconds the worker sleeps.
     */
    protected abstract Long getSuspendTime();

    /**
     * Returns true if the worker is running.
     * 
     * @return True if the worker is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns true if the worker is suspended/waiting.
     * 
     * @return True if the worker is suspended/waiting.
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Returns the {@link String} representation of the object.
     * 
     * @return The {@link String} representation of the object.
     */
    @Override
    public String toString() {
        return getThread().getName();
    }

    /**
     * Returns the thread object of this worker.
     * 
     * @return The thread object of this worker.
     */
    public Thread getThread() {
        return thread;
    }
}
