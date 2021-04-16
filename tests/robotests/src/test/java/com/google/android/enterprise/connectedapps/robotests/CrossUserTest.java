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
package com.google.android.enterprise.connectedapps.robotests;

import static com.google.android.enterprise.connectedapps.SharedTestUtilities.INTERACT_ACROSS_USERS;
import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.app.Service;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.enterprise.connectedapps.RobolectricTestUtilities;
import com.google.android.enterprise.connectedapps.TestScheduledExecutorService;
import com.google.android.enterprise.connectedapps.testapp.crossuser.ProfileTestCrossUserType;
import com.google.android.enterprise.connectedapps.testapp.crossuser.TestCrossUserConfiguration;
import com.google.android.enterprise.connectedapps.testapp.crossuser.TestCrossUserConnector;
import com.google.android.enterprise.connectedapps.testapp.crossuser.TestCrossUserStringCallbackListenerImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(minSdk = VERSION_CODES.O)
public class CrossUserTest {

  private static final String STRING = "String";

  private final Application context = ApplicationProvider.getApplicationContext();
  private final TestScheduledExecutorService scheduledExecutorService =
      new TestScheduledExecutorService();
  private final TestCrossUserConnector testCrossUserConnector =
      TestCrossUserConnector.create(context, scheduledExecutorService);
  private final RobolectricTestUtilities testUtilities =
      new RobolectricTestUtilities(testCrossUserConnector, scheduledExecutorService);
  private final ProfileTestCrossUserType profileCrossUserType =
      ProfileTestCrossUserType.create(testCrossUserConnector);
  private final TestCrossUserStringCallbackListenerImpl crossUserStringCallback =
      new TestCrossUserStringCallbackListenerImpl();

  @Before
  public void setUp() {
    Service profileAwareService = Robolectric.setupService(TestCrossUserConfiguration.getService());
    testUtilities.initTests();
    IBinder binder = profileAwareService.onBind(/* intent= */ null);
    testUtilities.setBinding(binder, TestCrossUserConnector.class.getName());
    testUtilities.createWorkUser();
    testUtilities.turnOnWorkProfile();
    testUtilities.setRunningOnPersonalProfile();
    testUtilities.setRequestsPermissions(INTERACT_ACROSS_USERS);
    testUtilities.grantPermissions(INTERACT_ACROSS_USERS);
    testCrossUserConnector.stopManualConnectionManagement();
  }

  @Test
  // This test covers all CrossUser annotations
  public void passArgumentToCallback_works() {
    testUtilities.turnOnWorkProfile();
    testUtilities.startConnectingAndWait();

    profileCrossUserType.current().passString(STRING, crossUserStringCallback);

    assertThat(crossUserStringCallback.stringCallbackValue).isEqualTo(STRING);
  }
}
