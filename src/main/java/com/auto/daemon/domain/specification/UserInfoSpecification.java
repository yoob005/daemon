package com.auto.daemon.domain.specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import com.auto.daemon.domain.entity.UserInfoEntity;

public class UserInfoSpecification {
	
	public static Specification<UserInfoEntity> equalAccessKey(String accessKey){
		return new Specification<UserInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				return criteriaBuilder.equal(root.get("accessKey"), accessKey);
			}
		};
	}
	
	public static Specification<UserInfoEntity> equalUseYn(String useYn){
		return new Specification<UserInfoEntity>() {
			@Override
			public Predicate toPredicate(Root<UserInfoEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				return criteriaBuilder.equal(root.get("useYn"), useYn);
			}
		};
	}
	
}
