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
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Map;

/**
 * Wrapper for reading & writing {@link ImmutableSetMultimap} instances from and to {@link Parcel}
 * instances.
 */
public final class ParcelableImmutableSetMultimap<E, F> implements Parcelable {

  private static final int NULL_SIZE = -1;
  private static final int KEY_TYPE_INDEX = 0;
  private static final int VALUE_TYPE_INDEX = 1;

  private final Bundler bundler;
  private final BundlerType type;
  private final ImmutableSetMultimap<E, F> setMultimap;

  /**
   * Create a wrapper for a given immutable setMultimap.
   *
   * <p>The passed in {@link Bundler} must be capable of bundling {@code E} and {@code F}.
   */
  public static <E, F> ParcelableImmutableSetMultimap<E, F> of(
      Bundler bundler, BundlerType type, ImmutableSetMultimap<E, F> setMultimap) {
    return new ParcelableImmutableSetMultimap<>(bundler, type, setMultimap);
  }

  public ImmutableSetMultimap<E, F> get() {
    return setMultimap;
  }

  private ParcelableImmutableSetMultimap(
      Bundler bundler, BundlerType type, ImmutableSetMultimap<E, F> setMultimap) {
    if (bundler == null || type == null) {
      throw new NullPointerException();
    }
    this.bundler = bundler;
    this.type = type;
    this.setMultimap = setMultimap;
  }

  private ParcelableImmutableSetMultimap(Parcel in) {
    bundler = in.readParcelable(Bundler.class.getClassLoader());
    int size = in.readInt();

    if (size == NULL_SIZE) {
      type = null;
      setMultimap = null;
      return;
    }

    ImmutableSetMultimap.Builder<E, F> setMultimapBuilder = ImmutableSetMultimap.builder();

    type = (BundlerType) in.readParcelable(Bundler.class.getClassLoader());
    if (size > 0) {
      BundlerType keyType = type.typeArguments().get(KEY_TYPE_INDEX);
      BundlerType valueType = type.typeArguments().get(VALUE_TYPE_INDEX);
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        E key = (E) bundler.readFromParcel(in, keyType);
        @SuppressWarnings("unchecked")
        F value = (F) bundler.readFromParcel(in, valueType);
        setMultimapBuilder.put(key, value);
      }
    }

    setMultimap = setMultimapBuilder.build();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bundler, flags);

    if (setMultimap == null) {
      dest.writeInt(NULL_SIZE);
      return;
    }

    dest.writeInt(setMultimap.size());
    dest.writeParcelable(type, flags);
    if (!setMultimap.isEmpty()) {
      BundlerType keyType = type.typeArguments().get(0);
      BundlerType valueType = type.typeArguments().get(1);

      for (Map.Entry<E, F> entry : setMultimap.entries()) {
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
  public static final Creator<ParcelableImmutableSetMultimap> CREATOR =
      new Creator<ParcelableImmutableSetMultimap>() {
        @Override
        public ParcelableImmutableSetMultimap createFromParcel(Parcel in) {
          return new ParcelableImmutableSetMultimap(in);
        }

        @Override
        public ParcelableImmutableSetMultimap[] newArray(int size) {
          return new ParcelableImmutableSetMultimap[size];
        }
      };
}
