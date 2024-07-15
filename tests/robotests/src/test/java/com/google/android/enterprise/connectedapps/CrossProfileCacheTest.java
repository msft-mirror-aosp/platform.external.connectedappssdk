package com.google.android.enterprise.connectedapps;

import static com.google.common.truth.Truth.assertThat;

import java.io.Serializable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class CrossProfileCacheTest {

  private static final String KEY1 = "key1";
  private static final String KEY2 = "key2";
  private static final Serializable value1 = 123;
  private static final Serializable value2 = false;

  CrossProfileCache cache = CrossProfileCache.getInstance();

  @Before
  public void setUp() {
    cache.clearAll();
  }

  @Test
  public void getSerializable_valueInMemCache_valueReturned() {
    cache.putSerializable(KEY1, value1);

    Serializable result = cache.getSerializable(KEY1);

    assertThat(result).isEqualTo(value1);
  }

  @Test
  public void getSerializable_valueNotInMemCache_nullReturned() {
    Serializable result = cache.getSerializable(KEY1);

    assertThat(result).isNull();
  }

  @Test
  public void putSerializable_valueAddedToMemCache() {
    cache.putSerializable(KEY1, value1);

    boolean inMemCache = cache.contains(KEY1);

    assertThat(inMemCache).isTrue();
  }

  @Test
  public void putSerializable_keyAlreadyUsed_newValueReplacedOldValue() {
    cache.putSerializable(KEY1, value1);
    cache.putSerializable(KEY1, value2);

    Serializable result = cache.getSerializable(KEY1);

    assertThat(result).isEqualTo(value2);
  }

  @Test
  public void putSerializable_cacheFull_newEntryAdded_oneEntryRemoved() {
    for (int i = 0; i < cache.maxNumberOfEntries(); i++) {
      cache.putSerializable(KEY1 + i, value1);
    }

    cache.putSerializable(KEY2, value2);

    assertThat(cache.numberOfEntries()).isEqualTo(cache.maxNumberOfEntries());
    assertThat(cache.contains(KEY2)).isTrue();
    // The least recently used entry should be evicted.
    assertThat(cache.contains(KEY1 + 0)).isFalse();
  }

  @Test
  public void remove_valueInMemCache_valueRemovedFromMemCache() {
    cache.putSerializable(KEY1, value1);

    cache.remove(KEY1);

    assertThat(cache.contains(KEY1)).isFalse();
  }

  @Test
  public void remove_valueNotInMemCache_nothingRemovedFromMemCache() {
    cache.putSerializable(KEY1, value1);

    cache.remove(KEY2);

    assertThat(cache.numberOfEntries()).isEqualTo(1);
  }

  @Test
  public void clearAll_allValuesRemovedFromMemCache() {
    cache.putSerializable(KEY1, value1);
    cache.putSerializable(KEY1, value2);

    cache.clearAll();

    assertThat(cache.contains(KEY1)).isFalse();
    assertThat(cache.contains(KEY2)).isFalse();
    assertThat(cache.numberOfEntries()).isEqualTo(0);
  }
}
