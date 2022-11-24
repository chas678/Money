package com.pobox.common.model;

import org.junit.jupiter.api.Test;

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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoneyThreadSafety {

    public static void assertConcurrent(final String message, final List<? extends Runnable> runnables,
                                        final int maxTimeoutSeconds) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables) {
                threadPool.submit(() -> {
                    allExecutorThreadsReady.countDown();
                    try {
                        afterInitBlocker.await();
                        submittedTestRunnable.run();
                    } catch (final Throwable e) {
                        exceptions.add(e);
                    } finally {
                        allDone.countDown();
                    }
                });
            }
            // wait until all threads are ready
            assertTrue(
                    allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS),
                    "Timeout initializing threads! Perform long lasting initializations before passing runnables to " +
                            "assertConcurrent");
            // start all test runners
            afterInitBlocker.countDown();
            assertTrue(allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS),
                    message + " timeout! More than" + maxTimeoutSeconds + "seconds");
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(exceptions.isEmpty(), message + "failed with exception(s)" + exceptions);
    }

    @Test
    public void testFormatThreadSafe() throws Exception {
        List<MoneyCheck> runners = IntStream.range(0, 1000)
                .mapToObj(i -> new MoneyCheck())
                .collect(Collectors.toList());
        assertConcurrent("NumberFormatFails", runners, 5);
    }

    static class MoneyCheck implements Runnable {

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
