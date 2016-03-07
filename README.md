# worker
An intelligent java thread implementation.

##Features
- Easy to use
- Does not wake up until needed
- Is scalable


##Example

```Java
import java.util.LinkedList;
import java.util.Queue;
    
public class TaskQueue {
    
    private static final int WORKER_COUNT = 5;
    private Queue<Task> queue;
    private QueueProcessor processor;
    
    public TaskQueue() {
        queue = new LinkedList<Task>();
        // create and setup workers.
        for (int i = 0; i < WORKER_COUNT; i++) {
            processor = new QueueProcessor();
            processor.start();
        }
    }

    public void addTask(Task task) {
        synchronized (queue) {
            queue.add(task);
        }
        // notify workers in case they're all sleeping.
        processor.wakeUpAllWorkers();
    }
    
    /**
     * A worker processing a queue of Task objects by extending AbstractWorker.
     */
    class QueueProcessor extends AbstractWorker<Task> {
    
        public QueueProcessor() {
            super("QueueProcessor");
        }
        
        /**
         * This method is called by the worker as soon as 
         * the isWorkAvailable method returns true.
         */
        @Override
        protected void doWork(Task task) {
            // do something with the task
        }
    
        /**
         * This method is used to fetch the next item to work with.
         * Important: This method must not block!
         */
        @Override
        protected Task getWork() {
            return queue.poll();
        }
    
        /**
         * Returns true if new work is available, false otherwise.
         * Important: This method must not block!
         */
        @Override
        protected boolean isWorkAvailable() {
            return !queue.isEmpty();
        }
    
        /**
         * Returns the amount of time in milliseconds that a worker 
         * sleeps when the isWorkAvailable method returns false. 
         * If null is returned the worker waits until notified.
         */
        @Override
        protected Long getSuspendTime() {
            return null;
        }
    }
}
```
