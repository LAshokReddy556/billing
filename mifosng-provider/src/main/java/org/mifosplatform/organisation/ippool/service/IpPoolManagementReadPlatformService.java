package org.mifosplatform.organisation.ippool.service;

import java.util.List;

import org.mifosplatform.crm.clientprospect.service.SearchSqlQuery;
import org.mifosplatform.infrastructure.core.service.Page;
import org.mifosplatform.organisation.ippool.data.IpPoolData;
import org.mifosplatform.organisation.ippool.data.IpPoolManagementData;
public interface IpPoolManagementReadPlatformService {
	

	List<IpPoolData> getUnallocatedIpAddressDetailds();
	Long checkIpAddress(String ipaddress);

	List<IpPoolManagementData> retrieveAllData();

	Page<IpPoolManagementData> retrieveIpPoolData(SearchSqlQuery searchItemDetails, String type);
	
	List<String> retrieveIpPoolIDArray(String query);




	

}