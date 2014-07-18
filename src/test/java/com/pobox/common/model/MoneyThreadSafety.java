package com.pobox.common.model;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class MoneyThreadSafety {

    @Test
    public void testFormatThreadSafe() throws Exception {
        List<MoneyCheck> runners = new ArrayList<MoneyCheck>();
        for (int i = 0; i < 1000; i++) {
            MoneyCheck moneyCheck = new MoneyCheck();
            runners.add(moneyCheck);
        }
        assertConcurrent("NumberFormatFails", runners, 5);
    }

    public static void assertConcurrent(final String message, final List<? extends Runnable> runnables,
            final int maxTimeoutSeconds) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables) {
                threadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        allExecutorThreadsReady.countDown();
                        try {
                            afterInitBlocker.await();
                            submittedTestRunnable.run();
                        } catch (final Throwable e) {
                            exceptions.add(e);
                        } finally {
                            allDone.countDown();
                        }
                    }
                });
            }
            // wait until all threads are ready
            assertTrue(
                    "Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent",
                    allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds",
                    allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
    }

    class MoneyCheck implements Runnable {

        @Override
        public void run() {
            Money aMoney = new Money(123.45, Currency.getInstance(Locale.FRANCE));
            Money bMoney = Money.dollars(11.98);
            NumberFormat nf2 = bMoney.getFormatter();
            NumberFormat nf1 = aMoney.getFormatter();
            assertThat(nf1.format(aMoney.getAmount().doubleValue()), is("123.45 EUR"));
            assertThat(nf2.format(bMoney.getAmount().doubleValue()), is("11.98 USD"));
            nf1 = aMoney.getFormatter();
            assertThat(nf1.format(aMoney.getAmount().doubleValue()), is("123.45 EUR"));
        }
    }
}
