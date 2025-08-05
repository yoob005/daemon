# daemon


-- auto.user_info definition

-- Drop table

-- DROP TABLE auto.user_info;

CREATE TABLE auto.user_info (
	access_key varchar(256) NOT NULL,
	secret_key varchar(256) NOT NULL,
	reg_dt timestamp NOT NULL,
	use_yn bpchar(1) NOT NULL DEFAULT 'N'::bpchar,
	CONSTRAINT "PK_user_info" PRIMARY KEY (access_key)
);
