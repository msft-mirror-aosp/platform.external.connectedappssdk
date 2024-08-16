package com.google.android.enterprise.connectedapps;

import android.util.LruCache;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.Serializable;

/** In-memory implementation of {@link Cache}. */
@CheckReturnValue // see go/why-crv
final class CrossProfileCache implements Cache {

  private static CrossProfileCache instance;

  private static final int DEFAULT_NUMBER_OF_ENTRIES = 10;

  private int maxNumberOfEntries = DEFAULT_NUMBER_OF_ENTRIES;
  private int numberOfEntries = 0;
  private final LruCache<String, Serializable> memCache = new LruCache<>(maxNumberOfEntries);

  private CrossProfileCache() {}

  /**
   * Returns the current instance of the cache or create a new one if one does not already exists.
   */
  public static synchronized CrossProfileCache getInstance() {
    if (instance == null) {
      instance = new CrossProfileCache();
    }
    return instance;
  }

  @Override
  public Serializable getSerializable(String key) {
    return memCache.get(key);
  }

  @Override
  public void putSerializable(String key, Serializable value) {
    memCache.put(key, value);
    if (numberOfEntries < maxNumberOfEntries) {
      numberOfEntries++;
    }
  }

  @Override
  public void remove(String key) {
    if (memCache.remove(key) != null) {
      numberOfEntries--;
    }
  }

  @Override
  public void clearAll() {
    memCache.evictAll();
    numberOfEntries = 0;
  }

  @Override
  public boolean contains(String key) {
    return this.getSerializable(key) != null;
  }

  /** Returns the current number of entries in the cache. */
  int numberOfEntries() {
    return numberOfEntries;
  }

  int maxNumberOfEntries() {
    return maxNumberOfEntries;
  }

  /** Changes the maximum number of entries in the cache. */
  public void resize(int maxNumberOfEntries) {
    instance.maxNumberOfEntries = maxNumberOfEntries;
    memCache.resize(maxNumberOfEntries);
  }
}
