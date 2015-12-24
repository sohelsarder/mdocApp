package com.mpower.daktar.android.models;

public class UserData {
	private String username = "";
	private String password = "";

	public void resetAll() {
		username = "";
		password = "";
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}
}
