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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

/**
 * Wrapper for reading & writing {@link ImmutableCollection} instances from and to {@link Parcel}
 * instances.
 */
public final class ParcelableImmutableCollection<E> implements Parcelable {

  private static final int NULL_SIZE = -1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableCollection<E> collection;

  /**
   * Create a wrapper for a given immutable collection.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E}
   */
  public static <E> ParcelableImmutableCollection<E> of(
      Bundler bundler, BundlerType type, ImmutableCollection<E> collection) {
    return new ParcelableImmutableCollection<>(bundler, type, collection);
  }

  public ImmutableCollection<E> get() {
    return collection;
  }

  private ParcelableImmutableCollection(
      Bundler bundler, BundlerType type, ImmutableCollection<E> collection) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.collection = collection;
  }

  private ParcelableImmutableCollection(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      collection = null;
      return;
    }

    ImmutableCollection.Builder<E> collectionBuilder = ImmutableList.builder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType elementType = type.typeArguments().get(0);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E element = (E) bundler.readFromParcel(in, elementType);
        collectionBuilder.add(element);
      }
    }

    collection = collectionBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (collection == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(collection.size());
    dest.writeParcelable(type, flags);
    if (!collection.isEmpty()) {
      BundlerType valueType = type.typeArguments().get(0);

      for (E value : collection) {
        bundler.writeToParcel(dest, value, valueType, flags);
      }
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("rawtypes")
  public static final Creator<ParcelableImmutableCollection> CREATOR =
      new Creator<ParcelableImmutableCollection>() {
        @Override
        public ParcelableImmutableCollection createFromParcel(Parcel in) {
          return new ParcelableImmutableCollection(in);
        }

        @Override
        public ParcelableImmutableCollection[] newArray(int size) {
          return new ParcelableImmutableCollection[size];
        }
      };
}
