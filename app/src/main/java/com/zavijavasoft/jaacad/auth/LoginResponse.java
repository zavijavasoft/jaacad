package com.zavijavasoft.jaacad.auth;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LoginResponse {

    @SerializedName("login")
    String login;

    @SerializedName("id")
    String id;

    @SerializedName("openid_identities")
    List<String> openIdIdentities;

    @SerializedName("emails")
    List<String> emails;

    @SerializedName("is_avatar_empty")
    boolean isAvatarEmpty;

    @SerializedName("default_avatar_id")
    String defaultAvatarId;

    @SerializedName("old_social_login")
    String oldSocialLogin;

    @SerializedName("birthday")
    String birthday;

    @SerializedName("sex")
    String gender;

    @SerializedName("first_name")
    String firstName;

    @SerializedName("last_name")
    String lastName;

    @SerializedName("display_name")
    String displayName;

    @SerializedName("real_name")
    String realName;

    public String getLogin() {
        return login;
    }

    public String getId() {
        return id;
    }

    public List<String> getOpenIdIdentities() {
        return openIdIdentities;
    }

    public List<String> getEmails() {
        return emails;
    }

    public boolean isAvatarEmpty() {
        return isAvatarEmpty;
    }

    public String getDefaultAvatarId() {
        return defaultAvatarId;
    }

    public String getOldSocialLogin() {
        return oldSocialLogin;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getGender() {
        return gender;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRealName() {
        return realName;
    }
}
