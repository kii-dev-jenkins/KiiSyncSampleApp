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
     *
     */
    public int register(String userName, String password, String country,
            String nickName, String mobile);
    
    /**
     * Log in using the given username and password. 
     * If the given password is different from previous one, it will login again
     * else just return OK.
     * 
     * @param username
     * @param password
     * @return
     */
    public int login(String username, String password);
    
    /**
     * Logout by clearing all the user information.
     * @return
     */
    public int logout();
    
}
