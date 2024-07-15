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
import com.google.common.collect.ImmutableSortedMultiset;

/**
 * Wrapper for reading & writing {@link ImmutableSortedMultiset} instances from and to {@link
 * Parcel} instances.
 */
public final class ParcelableImmutableSortedMultiset<E extends Comparable> implements Parcelable {

  private static final int NULL_SIZE = -1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableSortedMultiset<E> sortedMultiset;

  /**
   * Create a wrapper for a given immutable sorted set.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E}
   */
  public static <E extends Comparable> ParcelableImmutableSortedMultiset<E> of(
      Bundler bundler, BundlerType type, ImmutableSortedMultiset<E> sortedMultiset) {
    return new ParcelableImmutableSortedMultiset<E>(bundler, type, sortedMultiset);
  }

  public ImmutableSortedMultiset<E> get() {
    return sortedMultiset;
  }

  private ParcelableImmutableSortedMultiset(
      Bundler bundler, BundlerType type, ImmutableSortedMultiset<E> sortedMultiset) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.sortedMultiset = sortedMultiset;
  }

  private ParcelableImmutableSortedMultiset(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      sortedMultiset = null;
      return;
    }

    ImmutableSortedMultiset.Builder<E> sortedMultisetBuilder =
        ImmutableSortedMultiset.naturalOrder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType elementType = type.typeArguments().get(0);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E element = (E) bundler.readFromParcel(in, elementType);
        sortedMultisetBuilder.add(element);
      }
    }

    sortedMultiset = sortedMultisetBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (sortedMultiset == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(sortedMultiset.size());
    dest.writeParcelable(type, flags);
    if (!sortedMultiset.isEmpty()) {
      BundlerType valueType = type.typeArguments().get(0);

      for (E value : sortedMultiset) {
        bundler.writeToParcel(dest, value, valueType, flags);
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  public static final Creator<ParcelableImmutableSortedMultiset> CREATOR =
      new Creator<ParcelableImmutableSortedMultiset>() {
        @Override
        public ParcelableImmutableSortedMultiset createFromParcel(Parcel in) {
          return new ParcelableImmutableSortedMultiset(in);
        }

        @Override
        public ParcelableImmutableSortedMultiset[] newArray(int size) {
          return new ParcelableImmutableSortedMultiset[size];
        }
      };
}
