package org.mifosplatform.organisation.message.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingMessageTemplateRepository extends JpaRepository<BillingMessageTemplate, Long>, JpaSpecificationExecutor<BillingMessageTemplate> {
	
	@Query("from BillingMessageTemplate messageTemplate where messageTemplate.templateDescription =:templateDescription")
	BillingMessageTemplate findOneByTemplate(@Param("templateDescription")String templateDescription);
}
