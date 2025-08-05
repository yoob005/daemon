package com.auto.daemon.domain.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Data;

@Entity
@Table(name="user_info")
@Data
public class UserInfoEntity {
		
	@Id
	@Column(name = "access_key")
	private String accessKey;
	
	@Column(name = "secret_key")
	private String secretKey;
	
	@CreationTimestamp
	@Column(name = "reg_dt")
	private Timestamp regDt;
	
	@Column(name = "use_yn")
	private String useYn;	
	
}
