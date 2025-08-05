package com.auto.daemon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.auto.daemon.domain.entity.UserInfoEntity;
import com.auto.daemon.domain.repository.UserInfoRepository;
import com.auto.daemon.domain.specification.UserInfoSpecification;

@Service
public class UserInfoService {
	
	@Autowired(required=true)
	private UserInfoRepository repo;
	
	public UserInfoEntity getUserInfo(String accessKey ,String useYn) throws Exception{
		
		Specification<UserInfoEntity> spec = Specification.where(UserInfoSpecification.equalAccessKey(accessKey));
		spec = spec.and(UserInfoSpecification.equalUseYn(useYn));
		
		return repo.findByAccessKey(accessKey);
	}

}
