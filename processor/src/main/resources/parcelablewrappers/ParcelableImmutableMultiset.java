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
import com.google.common.collect.ImmutableMultiset;

/**
 * Wrapper for reading & writing {@link ImmutableMultiset} instances from and to {@link Parcel}
 * instances.
 */
public final class ParcelableImmutableMultiset<E> implements Parcelable {

  private static final int NULL_SIZE = -1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableMultiset<E> multiset;

  /**
   * Create a wrapper for a given immutable multiset.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E}
   */
  public static <E> ParcelableImmutableMultiset<E> of(
      Bundler bundler, BundlerType type, ImmutableMultiset<E> multiset) {
    return new ParcelableImmutableMultiset<>(bundler, type, multiset);
  }

  public ImmutableMultiset<E> get() {
    return multiset;
  }

  private ParcelableImmutableMultiset(
      Bundler bundler, BundlerType type, ImmutableMultiset<E> multiset) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.multiset = multiset;
  }

  private ParcelableImmutableMultiset(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      multiset = null;
      return;
    }

    ImmutableMultiset.Builder<E> multisetBuilder = ImmutableMultiset.builder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType elementType = type.typeArguments().get(0);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E element = (E) bundler.readFromParcel(in, elementType);
        multisetBuilder.add(element);
      }
    }

    multiset = multisetBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (multiset == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(multiset.size());
    dest.writeParcelable(type, flags);
    if (!multiset.isEmpty()) {
      BundlerType valueType = type.typeArguments().get(0);

      for (E value : multiset) {
        bundler.writeToParcel(dest, value, valueType, flags);
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  public static final Creator<ParcelableImmutableMultiset> CREATOR =
      new Creator<ParcelableImmutableMultiset>() {
        @Override
        public ParcelableImmutableMultiset createFromParcel(Parcel in) {
          return new ParcelableImmutableMultiset(in);
        }

        @Override
        public ParcelableImmutableMultiset[] newArray(int size) {
          return new ParcelableImmutableMultiset[size];
        }
      };
}
