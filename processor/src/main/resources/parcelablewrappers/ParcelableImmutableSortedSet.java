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
import com.google.common.collect.ImmutableSortedSet;

/**
 * Wrapper for reading & writing {@link ImmutableSortedSet} instances from and to {@link Parcel}
 * instances.
 */
public final class ParcelableImmutableSortedSet<E extends Comparable> implements Parcelable {

  private static final int NULL_SIZE = -1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableSortedSet<E> sortedSet;

  /**
   * Create a wrapper for a given immutable sorted set.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E}
   */
  public static <E extends Comparable> ParcelableImmutableSortedSet<E> of(
      Bundler bundler, BundlerType type, ImmutableSortedSet<E> sortedSet) {
    return new ParcelableImmutableSortedSet<E>(bundler, type, sortedSet);
  }

  public ImmutableSortedSet<E> get() {
    return sortedSet;
  }

  private ParcelableImmutableSortedSet(
      Bundler bundler, BundlerType type, ImmutableSortedSet<E> sortedSet) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.sortedSet = sortedSet;
  }

  private ParcelableImmutableSortedSet(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      sortedSet = null;
      return;
    }

    ImmutableSortedSet.Builder<E> sortedSetBuilder = ImmutableSortedSet.naturalOrder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType elementType = type.typeArguments().get(0);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E element = (E) bundler.readFromParcel(in, elementType);
        sortedSetBuilder.add(element);
      }
    }

    sortedSet = sortedSetBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (sortedSet == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(sortedSet.size());
    dest.writeParcelable(type, flags);
    if (!sortedSet.isEmpty()) {
      BundlerType valueType = type.typeArguments().get(0);

      for (E value : sortedSet) {
        bundler.writeToParcel(dest, value, valueType, flags);
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  public static final Creator<ParcelableImmutableSortedSet> CREATOR =
      new Creator<ParcelableImmutableSortedSet>() {
        @Override
        public ParcelableImmutableSortedSet createFromParcel(Parcel in) {
          return new ParcelableImmutableSortedSet(in);
        }

        @Override
        public ParcelableImmutableSortedSet[] newArray(int size) {
          return new ParcelableImmutableSortedSet[size];
        }
      };
}
