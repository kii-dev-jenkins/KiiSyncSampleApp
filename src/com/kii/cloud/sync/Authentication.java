//
//
//  Copyright 2012 Kii Corporation
//  http://kii.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  
//

package com.kii.cloud.sync;

public interface Authentication {

    /**
     * Change the password of the user.
     * 
     * @param oldPassword
     * @param newPassword
     */
    public int changePassword(String oldPassword, String newPassword);

    /**
     * Register user if no account has been registered
     * 
     * @param userName
     *            : user name for login
     * @param password
     *            : password (min length and max length)
     * @param country
     *            :
     * @param nickName
     *            :
     * @param mobile
     *            :
     * @return
     */
    public int register(String userName, String password, String country,
            String nickName, String mobile);

    /**
     * Log in using the given username and password. If the given password is
     * different from previous one, it will login again else just return OK.
     * 
     * @param username
     * @param password
     * @return
     */
    public int login(String username, String password);

    /**
     * Logout by clearing all the user information.
     * 
     * @return
     */
    public int logout();

}
