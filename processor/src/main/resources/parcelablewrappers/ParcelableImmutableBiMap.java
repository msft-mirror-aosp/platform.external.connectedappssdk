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
import com.google.common.collect.ImmutableBiMap;

/**
 * Wrapper for reading & writing {@link ImmutableBiMap} instances from and to {@link Parcel}
 * instances.
 */
public final class ParcelableImmutableBiMap<E, F> implements Parcelable {

  private static final int NULL_SIZE = -1;
  private static final int KEY_TYPE_INDEX = 0;
  private static final int VALUE_TYPE_INDEX = 1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableBiMap<E, F> biMap;

  /**
   * Create a wrapper for a given immutable biMap.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E} and {@code F}.
   */
  public static <E, F> ParcelableImmutableBiMap<E, F> of(
      Bundler bundler, BundlerType type, ImmutableBiMap<E, F> biMap) {
    return new ParcelableImmutableBiMap<>(bundler, type, biMap);
  }

  public ImmutableBiMap<E, F> get() {
    return biMap;
  }

  private ParcelableImmutableBiMap(Bundler bundler, BundlerType type, ImmutableBiMap<E, F> biMap) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.biMap = biMap;
  }

  private ParcelableImmutableBiMap(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      biMap = null;
      return;
    }

    ImmutableBiMap.Builder<E, F> biMapBuilder = ImmutableBiMap.builder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType keyType = type.typeArguments().get(KEY_TYPE_INDEX);
      BundlerType valueType = type.typeArguments().get(VALUE_TYPE_INDEX);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E key = (E) bundler.readFromParcel(in, keyType);
        @SuppressWarnings("unchecked")
        F value = (F) bundler.readFromParcel(in, valueType);
        biMapBuilder.put(key, value);
      }
    }

    biMap = biMapBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (biMap == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(biMap.size());
    dest.writeParcelable(type, flags);
    if (!biMap.isEmpty()) {
      BundlerType keyType = type.typeArguments().get(0);
      BundlerType valueType = type.typeArguments().get(1);

      for (ImmutableBiMap.Entry<E, F> entry : biMap.entrySet()) {
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
  public static final Creator<ParcelableImmutableBiMap> CREATOR =
      new Creator<ParcelableImmutableBiMap>() {
        @Override
        public ParcelableImmutableBiMap createFromParcel(Parcel in) {
          return new ParcelableImmutableBiMap(in);
        }

        @Override
        public ParcelableImmutableBiMap[] newArray(int size) {
          return new ParcelableImmutableBiMap[size];
        }
      };
}
