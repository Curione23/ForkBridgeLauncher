package ca.dnamobile.javalauncher.ui.version;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: /data/data/com.termux/files/home/jadx/classes.dex */
public final class ParallelDownloadExecutor {
    private static final int MAX_NETWORK_THREADS = 6;

    public interface Progress<T> {
        void onItemComplete(int i, int i2, T t);
    }

    public interface Worker<T> {
        void run(T t) throws Exception;
    }

    public static int defaultNetworkThreads() {
        return 6;
    }

    private ParallelDownloadExecutor() {
    }

    public static <T> void run(final List<T> list, int i, final Worker<T> worker, final Progress<T> progress) throws Exception {
        if (list.isEmpty()) {
            return;
        }
        ExecutorService executorServiceNewFixedThreadPool = Executors.newFixedThreadPool(Math.max(1, Math.min(Math.min(i, list.size()), 6)), new ThreadFactory() { // from class: ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor.1
            private final AtomicInteger nextId = new AtomicInteger(1);

            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "Installer download #" + this.nextId.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        ArrayList arrayList = new ArrayList(list.size());
        try {
            try {
                for (final T t : list) {
                    arrayList.add(executorServiceNewFixedThreadPool.submit(new Callable() { // from class: ca.dnamobile.javalauncher.ui.version.ParallelDownloadExecutor$$ExternalSyntheticLambda0
                        @Override // java.util.concurrent.Callable
                        public final Object call() {
                            return ParallelDownloadExecutor.lambda$run$0(worker, t, atomicInteger, progress, list);
                        }
                    }));
                }
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    ((Future) it.next()).get();
                }
            } catch (InterruptedException e) {
                cancelAll(arrayList);
                Thread.currentThread().interrupt();
                throw e;
            } catch (ExecutionException e2) {
                cancelAll(arrayList);
                Throwable cause = e2.getCause();
                if (cause instanceof Exception) {
                    throw ((Exception) cause);
                }
                if (!(cause instanceof Error)) {
                    throw new Exception(cause);
                }
                throw ((Error) cause);
            }
        } finally {
            executorServiceNewFixedThreadPool.shutdownNow();
        }
    }

    static /* synthetic */ Object lambda$run$0(Worker worker, Object obj, AtomicInteger atomicInteger, Progress progress, List list) throws Exception {
        worker.run(obj);
        int iIncrementAndGet = atomicInteger.incrementAndGet();
        if (progress == null) {
            return null;
        }
        progress.onItemComplete(iIncrementAndGet, list.size(), obj);
        return null;
    }

    private static void cancelAll(ArrayList<Future<?>> arrayList) {
        Iterator<Future<?>> it = arrayList.iterator();
        while (it.hasNext()) {
            it.next().cancel(true);
        }
    }
}
