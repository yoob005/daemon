package com.auto.daemon;

import com.zaxxer.hikari.HikariDataSource;

public class MyHikariDataSource extends HikariDataSource {

	@Override
	public void setPassword(String password) {
		super.setPassword(password);
	}
	
	@Override
	public void setUsername(String username) {
		super.setUsername(username);
	}
}
