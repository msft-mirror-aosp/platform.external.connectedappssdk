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
package com.google.android.enterprise.connectedapps.parcelablewrappers;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.enterprise.connectedapps.internal.Bundler;
import com.google.android.enterprise.connectedapps.internal.BundlerType;
import com.google.common.collect.ImmutableSortedMap;

/**
 * Wrapper for reading & writing {@link ImmutableSortedMap} instances from and to {@link Parcel}
 * instances.
 */
public final class ParcelableImmutableSortedMap<E extends Comparable, F> implements Parcelable {

  private static final int NULL_SIZE = -1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableSortedMap<E, F> sortedMap;

  /**
   * Create a wrapper for a given immutable sorted map.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E}
   */
  public static <E extends Comparable, F> ParcelableImmutableSortedMap<E, F> of(
      Bundler bundler, BundlerType type, ImmutableSortedMap<E, F> sortedMap) {
    return new ParcelableImmutableSortedMap<E, F>(bundler, type, sortedMap);
  }

  public ImmutableSortedMap<E, F> get() {
    return sortedMap;
  }

  private ParcelableImmutableSortedMap(
      Bundler bundler, BundlerType type, ImmutableSortedMap<E, F> sortedMap) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.sortedMap = sortedMap;
  }

  private ParcelableImmutableSortedMap(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      sortedMap = null;
      return;
    }

    ImmutableSortedMap.Builder<E, F> sortedMapBuilder = ImmutableSortedMap.naturalOrder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType elementType = type.typeArguments().get(0);
      BundlerType valueType = type.typeArguments().get(1);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E key = (E) bundler.readFromParcel(in, elementType);
        @SuppressWarnings("unchecked")
        F value = (F) bundler.readFromParcel(in, valueType);
        sortedMapBuilder.put(key, value);
      }
    }

    sortedMap = sortedMapBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (sortedMap == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(sortedMap.size());
    dest.writeParcelable(type, flags);
    if (!sortedMap.isEmpty()) {
      BundlerType valueType = type.typeArguments().get(0);
      BundlerType keyType = type.typeArguments().get(1);

      for (ImmutableSortedMap.Entry<E, F> entry : sortedMap.entrySet()) {
        bundler.writeToParcel(dest, entry.getKey(), keyType, flags);
        bundler.writeToParcel(dest, entry.getValue(), valueType, flags);
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  public static final Creator<ParcelableImmutableSortedMap> CREATOR =
      new Creator<ParcelableImmutableSortedMap>() {
        @Override
        public ParcelableImmutableSortedMap createFromParcel(Parcel in) {
          return new ParcelableImmutableSortedMap(in);
        }

        @Override
        public ParcelableImmutableSortedMap[] newArray(int size) {
          return new ParcelableImmutableSortedMap[size];
        }
      };
}
