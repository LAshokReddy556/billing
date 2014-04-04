package org.mifosplatform.billing.ippool.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;

public interface IpPoolManagementWritePlatformService {

	CommandProcessingResult createIpPoolManagement(JsonCommand command);
	

}
