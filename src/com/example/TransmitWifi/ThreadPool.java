package com.example.TransmitWifi;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class ThreadPool {
    private static final String TAG = "ThreadPool";
    private static int mSeqNumber = 0;
    private final int mCount;
    private final List<Runnable> mWorkQueue = new LinkedList<Runnable>();
    private List<WorkThread> mThreadQueue = new LinkedList<WorkThread>();

    ThreadPool(int count) {
        mCount = count;
    }

    class WorkThread extends Thread {
        private int mThreadId = mSeqNumber++;
        private boolean mStop = false;
        public void run() {
            while (!mStop) {
                Runnable tasklet = null;
                synchronized (mWorkQueue) {
                    while (mWorkQueue.isEmpty()) {
                        Log.i(TAG, " enter wait state id = " + mThreadId);
                        try {
                            mWorkQueue.wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                            if (mStop) {
                                break;
                            } else {
                                Log.i(TAG, "Get a tasklet");
                            }
                        }
                    }
                    Log.i(TAG, "get pid =  " + getId());
                    Log.i(TAG, mThreadId + " get the work");
                    tasklet = mWorkQueue.get(0);
                    mWorkQueue.remove(0);
                }
                Log.i(TAG, "#####################enter###########");
                tasklet.run();
                Log.i(TAG, "#####################exit###########");
            }
        }
        public void exit() {
            mStop = true;
            this.interrupt();
        }
    }

    public void start() {
        int i = 0;
        for (i = 0; i < mCount; i++) {
            WorkThread thread = new WorkThread();
            thread.setPriority(i+Thread.NORM_PRIORITY);
            thread.start();
            mThreadQueue.add(thread);
        }
    }

    public void stop() {
        int i = 0;
        for (i = 0; i < mCount; i++) {
            WorkThread thread = mThreadQueue.get(0);
            thread.exit();
            try {
                thread.join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public void addTasklet(Runnable tasklet) {
        synchronized (mWorkQueue) {
            mWorkQueue.add(tasklet);
            mWorkQueue.notifyAll();
        }
    }
}



