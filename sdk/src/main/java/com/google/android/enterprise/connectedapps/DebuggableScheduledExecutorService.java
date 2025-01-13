/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.enterprise.connectedapps;

import android.util.Log;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//TODO(olit) remove this when b/371963670 is fixed
/**
 * A ScheduledExecutorService wrapper that adds logging for debugging purposes.
 *
 * <p>This class is intended to be used for debugging only and should be removed once
 * b/371963670 is fixed.
 *
 */
final class DebuggableScheduledExecutorService implements ScheduledExecutorService {

  private static final String LOG_TAG = "DebugExecutorService";
  private final ScheduledExecutorService real;

  /**
   * @param real The underlying ScheduledExecutorService to wrap.
   */
  public DebuggableScheduledExecutorService(ScheduledExecutorService real) {
    this.real = real;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return real.schedule(command, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return real.schedule(callable, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      Runnable command, long initialDelay, long period, TimeUnit unit) {
    return real.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(
      Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return real.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  @Override
  public void shutdown() {
    Log.i(LOG_TAG, "shutdown() called");
    real.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    Log.i(LOG_TAG, "shutdownNow() called");
    return real.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    Log.i(LOG_TAG, "isShutdown() called");
    return real.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    Log.i(LOG_TAG, "isTerminated() called");
    return real.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    Log.i(LOG_TAG, "awaitTermination() called with timeout: " + timeout + ", unit: " + unit);
    return real.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    Log.i(LOG_TAG, "submit() called with Callable: " + task);
    return real.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    Log.i(LOG_TAG, "submit() called with Runnable: " + task + ", result: " + result);
    return real.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    Log.i(LOG_TAG, "submit() called with Runnable: " + task);
    return real.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    Log.i(LOG_TAG, "invokeAll() called with tasks: " + tasks);
    return real.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    Log.i(
        LOG_TAG,
        "invokeAll() called with tasks: " + tasks + ", timeout: " + timeout + ", unit: " + unit);
    return real.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws ExecutionException, InterruptedException {
    Log.i(LOG_TAG, "invokeAny() called with tasks: " + tasks);
    return real.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws ExecutionException, InterruptedException, TimeoutException {
    Log.i(
        LOG_TAG,
        "invokeAny() called with tasks: " + tasks + ", timeout: " + timeout + ", unit: " + unit);
    return real.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    Log.i(LOG_TAG, "execute() called with Runnable: " + command);
    real.execute(command);
    Log.i(LOG_TAG, command + " finished on thread");
  }
}
