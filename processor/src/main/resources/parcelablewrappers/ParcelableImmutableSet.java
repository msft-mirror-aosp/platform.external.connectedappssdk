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
import com.google.common.collect.ImmutableSet;

/**
 * Wrapper for reading & writing {@link ImmutableSet} instances from and to {@link Parcel}
 * instances.
 */
public final class ParcelableImmutableSet<E> implements Parcelable {

  private static final int NULL_SIZE = -1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableSet<E> set;

  /**
   * Create a wrapper for a given immutable set.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E}
   */
  public static <E> ParcelableImmutableSet<E> of(
      Bundler bundler, BundlerType type, ImmutableSet<E> set) {
    return new ParcelableImmutableSet<>(bundler, type, set);
  }

  public ImmutableSet<E> get() {
    return set;
  }

  private ParcelableImmutableSet(Bundler bundler, BundlerType type, ImmutableSet<E> set) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.set = set;
  }

  private ParcelableImmutableSet(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      set = null;
      return;
    }

    ImmutableSet.Builder<E> setBuilder = ImmutableSet.builder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType elementType = type.typeArguments().get(0);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E element = (E) bundler.readFromParcel(in, elementType);
        setBuilder.add(element);
      }
    }

    set = setBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (set == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(set.size());
    dest.writeParcelable(type, flags);
    if (!set.isEmpty()) {
      BundlerType valueType = type.typeArguments().get(0);

      for (E value : set) {
        bundler.writeToParcel(dest, value, valueType, flags);
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  public static final Creator<ParcelableImmutableSet> CREATOR =
      new Creator<ParcelableImmutableSet>() {
        @Override
        public ParcelableImmutableSet createFromParcel(Parcel in) {
          return new ParcelableImmutableSet(in);
        }

        @Override
        public ParcelableImmutableSet[] newArray(int size) {
          return new ParcelableImmutableSet[size];
        }
      };
}
