package com.auto.daemon.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.auto.daemon.domain.entity.UserInfoEntity;

public interface UserInfoRepository extends JpaRepository<UserInfoEntity, String>, JpaSpecificationExecutor<UserInfoEntity>{
	UserInfoEntity findByAccessKey(String accessKey) throws Exception;
}
