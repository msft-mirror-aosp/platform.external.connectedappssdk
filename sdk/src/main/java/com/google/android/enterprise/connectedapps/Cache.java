package com.google.android.enterprise.connectedapps;

import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A cache of arbitrary {@link Serializable} values. */
public interface Cache {
  /**
   * Returns a Serializable value associated with a {@code key} from the cache or null if no such
   * value exists.
   */
  @Nullable Serializable getSerializable(String key);

  /** Adds a serializable {@code value} to the cache at {@code key}. */
  void putSerializable(String key, Serializable value);

  /** Deletes an entry in the cache at {@code key}. */
  void remove(String key);

  /** Deletes all entries in the cache. */
  void clearAll();

  /** Returns {@code true} if there is an entry in the cache at {@code key}. */
  boolean contains(String key);
}
