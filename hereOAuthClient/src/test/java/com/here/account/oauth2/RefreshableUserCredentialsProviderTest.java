/*
 * Copyright 2016 HERE Global B.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.junit.Before;
import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.PasswordGrantRequest;
import com.here.account.util.TestClock;
import com.here.account.util.RefreshableResponseProvider;

public class RefreshableUserCredentialsProviderTest extends AbstractUserTezt {
    
    RefreshableResponseProvider<AccessTokenResponse> refreshableResponseProvider;

    TestClock clock;
    
    @Before
    public void setUp() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        super.setUp();
        
        PasswordGrantRequest passwordGrantRequest = new PasswordGrantRequest().setEmail(email).setPassword(password);
        AccessTokenResponse accessTokenResponse = 
                signIn.signIn(passwordGrantRequest);
        Long refreshIntervalMillis = null;
        clock = new TestClock();
        refreshableResponseProvider = new RefreshableResponseProvider<AccessTokenResponse>(
                clock,
                refreshIntervalMillis,
                accessTokenResponse,
                new UserCredentialsRefresher(signIn),
                Executors.newScheduledThreadPool(
                        1, new ThreadFactory() {

                            @Override
                            public Thread newThread(Runnable r) {
                                Thread thread = new Thread(r, "here-test-auth-refresh-%s");
                                thread.setDaemon(true);
                                return thread;
                            }
                      
                        }
                        )
                );
    }
    
    @Test
    public void test_init() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        String accessToken = refreshableResponseProvider.getUnexpiredResponse().getAccessToken();
        assertTrue("accessToken was null", null != accessToken && accessToken.trim().length() > 0);
    }
    
    @Test
    public void test_forceRefresh() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        AccessTokenResponse accessTokenResponse = refreshableResponseProvider.getUnexpiredResponse();
        String accessToken = accessTokenResponse.getAccessToken();
        assertTrue("accessToken was null", null != accessToken && accessToken.trim().length() > 0);
        long expiresIn = accessTokenResponse.getExpiresIn();
        assertTrue("expiresIn was not positive: "+expiresIn, expiresIn > 0L);
        System.out.println("expiresIn "+expiresIn);
        clock.setCurrentTimeMillis(clock.currentTimeMillis() + expiresIn * 1000L - 60 * 1000L);
        AccessTokenResponse accessTokenResponse2 = refreshableResponseProvider.getUnexpiredResponse();
        String accessToken2 = accessTokenResponse2.getAccessToken();
        assertTrue("accessToken was expected "+accessToken+", actual "+accessToken2, 
                accessToken.equals(accessToken2));
        clock.setCurrentTimeMillis(clock.currentTimeMillis() + 90 * 1000L);
        AccessTokenResponse accessTokenResponse3 = refreshableResponseProvider.getUnexpiredResponse();
        String accessToken3 = accessTokenResponse3.getAccessToken();
        assertTrue("accessToken was expected !.equal to "+accessToken+", actual "+accessToken3, 
                !accessToken.equals(accessToken3));

        
    }

}
