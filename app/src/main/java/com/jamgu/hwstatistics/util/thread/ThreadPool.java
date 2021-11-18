package com.jamgu.hwstatistics.util.thread;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPool {
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 5;
    private static final int KEEP_ALIVE_TIME = 20;
    public static final int MODE_NONE = 0;
    public static final int MODE_CPU = 1;
    public static final int MODE_NETWORK = 2;
    ResourceCounter mCpuCounter;
    ResourceCounter mNetworkCounter;
    private static AtomicLong sSeqNoGen = new AtomicLong(0L);
    private ThreadPoolMonitor mPoolMonitor;
    private final Executor mExecutor;
    private int coreSize;
    private int maxSize;
    private static Handler UI_HANDLER = new Handler(Looper.getMainLooper());
    private static volatile ThreadPool sThreadPoolIO;
    private static volatile ThreadPool sThreadPoolNetwork;

    public ThreadPool() {
        this("bible-pool", 2, 5);
    }

    public ThreadPool(String name, int coreSize, int maxSize) {
        this(name, coreSize, maxSize, 20L, TimeUnit.SECONDS, new PriorityBlockingQueue());
    }

    public ThreadPool(String name, int coreSize, int maxSize, long keepAliveTime, TimeUnit unit) {
        this(name, coreSize, maxSize, keepAliveTime, unit, new PriorityBlockingQueue());
    }

    public ThreadPool(
            String name, int coreSize, int maxSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> queue) {
        this.mCpuCounter = new ResourceCounter(2);
        this.mNetworkCounter = new ResourceCounter(2);
        if (maxSize <= coreSize) {
            maxSize = coreSize;
        }

        this.maxSize = maxSize;
        this.coreSize = coreSize;
        this.mExecutor = new ThreadPoolExecutor(coreSize, maxSize, keepAliveTime, unit, queue, new PriorityThreadFactory(name, 10));
    }

    public void setPoolMonitor(ThreadPoolMonitor poolMonitor) {
        this.mPoolMonitor = poolMonitor;
    }

    public <T> Future<T> submit(Job<T> job, FutureListener<T> listener, Priority priority) {
        long seqNO = -1L;
        if (job instanceof RunnableJob) {
            seqNO = ((RunnableJob)job).seqNO;
        }

        if (seqNO < 0L) {
            seqNO = sSeqNoGen.getAndIncrement();
        }

        Worker<T> w = this.generateWorker(seqNO, job, listener, priority);
        if (this.mPoolMonitor != null) {
            this.mPoolMonitor.onSubmitWorker(seqNO, this, w);
        }

        this.mExecutor.execute(w);
        return w;
    }

    public <T> Future<T> submit(Job<T> job, FutureListener<T> listener) {
        return this.submit(job, listener, Priority.NORMAL);
    }

    public <T> Future<T> submit(Job<T> job, Priority priority) {
        return this.submit(job, (FutureListener)null, priority);
    }

    public <T> Future<T> submit(Job<T> job) {
        return this.submit(job, (FutureListener)null, Priority.NORMAL);
    }

    private <T> Worker<T> generateWorker(long seqNo, Job<T> job, FutureListener<T> listener, Priority priority) {
        PriorityWorker worker;
        switch(priority) {
            case LOW:
                worker = new PriorityWorker(job, listener, priority.priorityInt, false);
                break;
            case NORMAL:
                worker = new PriorityWorker(job, listener, priority.priorityInt, false);
                break;
            case HIGH:
                worker = new PriorityWorker(job, listener, priority.priorityInt, true);
                break;
            default:
                worker = new PriorityWorker(job, listener, priority.priorityInt, false);
        }

        worker.seqNo = seqNo;
        return worker;
    }

    private void notifyBeforeSubmitWorker(RunnableJob runnableJob) {
        if (this.mPoolMonitor != null) {
            this.mPoolMonitor.beforeSubmitWorker(runnableJob.seqNO, this, runnableJob.runnable);
        }

    }

    public static ThreadPool getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static void runOnNonUIThread(Runnable runnable) {
        runOnNonUIThread(runnable, Priority.NORMAL);
    }

    public static void runOnNonUIThread(Runnable runnable, Priority priority) {
        RunnableJob runnableJob = new RunnableJob(runnable);
        getInstance().notifyBeforeSubmitWorker(runnableJob);
        getInstance().submit(runnableJob, (Priority)priority);
    }

    public static void runOnNonUIThread(Runnable runnable, long delay) {
        final RunnableJob runnableJob = new RunnableJob(runnable);
        getInstance().notifyBeforeSubmitWorker(runnableJob);
        UI_HANDLER.postDelayed(new Runnable() {
            public void run() {
                ThreadPool.getInstance().submit(runnableJob, (Priority) Priority.NORMAL);
            }
        }, delay);
    }

    public static void runUITask(Runnable runnable) {
        UI_HANDLER.post(runnable);
    }

    public static void runUITask(Runnable runable, long delayMillis) {
        UI_HANDLER.postDelayed(runable, delayMillis);
    }

    public static void removeUITask(Runnable runnable) {
        UI_HANDLER.removeCallbacks(runnable);
    }

    public static ThreadPool getIOThreadPool() {
        if (sThreadPoolIO == null) {
            Class var0 = ThreadPool.class;
            synchronized(ThreadPool.class) {
                if (sThreadPoolIO == null) {
                    sThreadPoolIO = new ThreadPool("io-thread-pool", 3, 5);
                }
            }
        }

        return sThreadPoolIO;
    }

    public static void runIOTask(Runnable runnable) {
        if (runnable != null) {
            RunnableJob runnableJob = new RunnableJob(runnable);
            getIOThreadPool().notifyBeforeSubmitWorker(runnableJob);
            getIOThreadPool().submit(runnableJob);
        }

    }

    public static ThreadPool getNetworkThreadPool() {
        if (sThreadPoolNetwork == null) {
            Class var0 = ThreadPool.class;
            synchronized(ThreadPool.class) {
                if (sThreadPoolNetwork == null) {
                    sThreadPoolNetwork = new ThreadPool("network-thread-pool", 6, 6);
                }
            }
        }

        return sThreadPoolNetwork;
    }

    public static void runNetworkTask(Runnable runnable) {
        if (runnable != null) {
            RunnableJob runnableJob = new RunnableJob(runnable);
            getNetworkThreadPool().notifyBeforeSubmitWorker(runnableJob);
            getNetworkThreadPool().submit(runnableJob);
        }

    }

    public String toString() {
        return "ThreadPool { corePoolSize:" + this.coreSize + " maxPoolSize:" + this.maxSize + " mExecutor=" + this.mExecutor + '}';
    }

    public static Executor newSerialExecutor() {
        return new SerialExecutor();
    }

    private static class SerialExecutor implements Executor {
        final Queue<Runnable> mTasks;
        Runnable mActive;

        private SerialExecutor() {
            this.mTasks = new LinkedList();
        }

        public synchronized void execute(final Runnable r) {
            this.mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        SerialExecutor.this.scheduleNext();
                    }

                }
            });
            if (this.mActive == null) {
                this.scheduleNext();
            }

        }

        protected synchronized void scheduleNext() {
            if ((this.mActive = (Runnable)this.mTasks.poll()) != null) {
                ThreadPool.runOnNonUIThread(this.mActive);
            }

        }
    }

    private static class RunnableJob implements Job<Object> {
        long seqNO;
        Runnable runnable;

        RunnableJob(Runnable runnable) {
            this.runnable = runnable;
            this.seqNO = ThreadPool.sSeqNoGen.getAndIncrement();
        }

        public Object run(JobContext jc) {
            if (this.runnable != null) {
                this.runnable.run();
            }

            return null;
        }
    }

    private static class InstanceHolder {
        public static final ThreadPool INSTANCE = new ThreadPool();

        private InstanceHolder() {
        }
    }

    private class PriorityWorker<T> extends Worker<T> implements Comparable<PriorityWorker> {
        private final int mPriority;
        private final boolean mFilo;

        public PriorityWorker(Job<T> job, FutureListener<T> listener, int priority, boolean filo) {
            super(job, listener);
            this.mPriority = priority;
            this.mFilo = filo;
        }

        public int compareTo(PriorityWorker another) {
            return this.mPriority > another.mPriority ? -1 : (this.mPriority < another.mPriority ? 1 : this.subCompareTo(another));
        }

        private int subCompareTo(PriorityWorker another) {
            int result = this.seqNo < another.seqNo ? -1 : (this.seqNo > another.seqNo ? 1 : 0);
            return this.mFilo ? -result : result;
        }
    }

    private class Worker<T> implements Runnable, Future<T>, JobContext {
        private static final String TAG = "Worker";
        private Job<T> mJob;
        private FutureListener<T> mListener;
        private Future.CancelListener mCancelListener;
        private ResourceCounter mWaitOnResource;
        private volatile boolean mIsCancelled;
        private boolean mIsDone;
        private T mResult;
        private int mMode;
        protected long seqNo = -1L;

        public Worker(Job<T> job, FutureListener<T> listener) {
            this.mJob = job;
            this.mListener = listener;
        }

        public void run() {
            if (this.mListener != null) {
                this.mListener.onFutureBegin(this);
            }

            if (ThreadPool.this.mPoolMonitor != null) {
                ThreadPool.this.mPoolMonitor.onWorkerBegin(this.seqNo, ThreadPool.this, this);
            }

            T result = null;
            if (this.setMode(1)) {
                result = this.mJob.run(this);
            }

            synchronized(this) {
                this.setMode(0);
                this.mResult = result;
                this.mIsDone = true;
                this.notifyAll();
            }

            if (ThreadPool.this.mPoolMonitor != null) {
                ThreadPool.this.mPoolMonitor.onWorkerFinish(this.seqNo, ThreadPool.this, this);
            }

            if (this.mListener != null) {
                this.mListener.onFutureDone(this);
            }

        }

        public synchronized void cancel() {
            if (!this.mIsCancelled) {
                this.mIsCancelled = true;
                if (this.mWaitOnResource != null) {
                    synchronized(this.mWaitOnResource) {
                        this.mWaitOnResource.notifyAll();
                    }
                }

                if (ThreadPool.this.mPoolMonitor != null) {
                    ThreadPool.this.mPoolMonitor.onWorkerCancel(this.seqNo, ThreadPool.this, this);
                }

                if (this.mCancelListener != null) {
                    this.mCancelListener.onCancel();
                }

            }
        }

        public boolean isCancelled() {
            return this.mIsCancelled;
        }

        public synchronized boolean isDone() {
            return this.mIsDone;
        }

        public synchronized T get() {
            while(!this.mIsDone) {
                try {
                    this.wait();
                } catch (Exception var2) {
                    Log.w("Worker", "ignore exception", var2);
                }
            }

            return this.mResult;
        }

        public void waitDone() {
            this.get();
        }

        public synchronized void setCancelListener(CancelListener listener) {
            this.mCancelListener = listener;
            if (this.mIsCancelled && this.mCancelListener != null) {
                this.mCancelListener.onCancel();
            }

        }

        public boolean setMode(int mode) {
            ResourceCounter rc = this.modeToCounter(this.mMode);
            if (rc != null) {
                this.releaseResource(rc);
            }

            this.mMode = 0;
            rc = this.modeToCounter(mode);
            if (rc != null) {
                if (!this.acquireResource(rc)) {
                    return false;
                }

                this.mMode = mode;
            }

            return true;
        }

        private ResourceCounter modeToCounter(int mode) {
            if (mode == 1) {
                return ThreadPool.this.mCpuCounter;
            } else {
                return mode == 2 ? ThreadPool.this.mNetworkCounter : null;
            }
        }

        private boolean acquireResource(ResourceCounter counter) {
            while(true) {
                synchronized(this) {
                    if (this.mIsCancelled) {
                        this.mWaitOnResource = null;
                        return false;
                    }

                    this.mWaitOnResource = counter;
                }

                synchronized(counter) {
                    if (counter.value <= 0) {
                        try {
                            counter.wait();
                        } catch (InterruptedException var7) {
                        }
                        continue;
                    }

                    --counter.value;
                }

                synchronized(this) {
                    this.mWaitOnResource = null;
                    return true;
                }
            }
        }

        private void releaseResource(ResourceCounter counter) {
            synchronized(counter) {
                ++counter.value;
                counter.notifyAll();
            }
        }
    }

    public static enum Priority {
        LOW(1),
        NORMAL(2),
        HIGH(3);

        int priorityInt;

        private Priority(int priority) {
            this.priorityInt = priority;
        }
    }

    private static class ResourceCounter {
        public int value;

        public ResourceCounter(int v) {
            this.value = v;
        }
    }

    public interface JobContext {
        boolean isCancelled();

        boolean setMode(int var1);
    }

    public interface Job<T> {
        T run(JobContext var1);
    }
}
