package com.traker.traker.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.QueryByExampleExecutor;

@NoRepositoryBean
public interface DefaultRepository<ENT, ID> extends JpaRepository<ENT, ID>, QueryByExampleExecutor<ENT> {
}
