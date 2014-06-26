package org.mifosplatform.organisation.ippool.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.mifosplatform.billing.pricing.service.PriceReadPlatformService;
import org.mifosplatform.crm.clientprospect.service.SearchSqlQuery;
import org.mifosplatform.infrastructure.core.service.Page;
import org.mifosplatform.infrastructure.core.service.PaginationHelper;
import org.mifosplatform.infrastructure.core.service.TenantAwareRoutingDataSource;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.ippool.data.IpPoolData;
import org.mifosplatform.organisation.ippool.data.IpPoolManagementData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
//String serviceDescription = rs.getString("service_description");
// TODO Auto-generated method stub
@Service
public class IpPoolManagementReadPlatformServiceImpl implements IpPoolManagementReadPlatformService {


	private final JdbcTemplate jdbcTemplate;
	private final PlatformSecurityContext context;
	private final PaginationHelper<IpPoolManagementData> paginationHelper = new PaginationHelper<IpPoolManagementData>(); 
	
	

	@Autowired
	public IpPoolManagementReadPlatformServiceImpl(final PlatformSecurityContext context,final PriceReadPlatformService priceReadPlatformService,
			final TenantAwareRoutingDataSource dataSource) {
		this.context = context;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public List<IpPoolData> getUnallocatedIpAddressDetailds() {

		context.authenticatedUser();
		ProvisioningMapper mapper = new ProvisioningMapper();

		String sql = "select " + mapper.schema();

		return this.jdbcTemplate.query(sql, mapper, new Object[] {});

	}

	private static final class ProvisioningMapper implements RowMapper<IpPoolData> {

		public String schema() {
			return " pd.id as id,pd.pool_name as poolName,pd.ip_address as ipaddress  from b_ippool_details pd where status='F'";

		}

		@Override
		public IpPoolData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

			Long id = rs.getLong("id");
			String poolName = rs.getString("poolName");
			String ipaddress = rs.getString("ipaddress");
			//String serviceDescription = rs.getString("service_description");
			return new IpPoolData(id,poolName,ipaddress);

		}
	}

	

	@Override
	public Long checkIpAddress(String ipaddress) {
		try {
			String sql = "select b.id as id from b_ippool_details b where b.ip_address = '"+ ipaddress + "' ";
			return jdbcTemplate.queryForLong(sql);
		} catch (EmptyResultDataAccessException e) {
			return null;
		} 
	}

	/*@Override
	public List<IpPoolManagementData> retrieveAllData() {
		IpPoolMapper mapper = new IpPoolMapper();
		String sql = "select " + mapper.schema();
		return this.jdbcTemplate.query(sql, mapper, new Object[] {});
	}*/

	private static final class IpPoolMapper implements
			RowMapper<IpPoolManagementData> {

		public String schema() {

			//return "p.id ,p.pool_name as poolName, p.ip_address as ipAddress ,p.status, p.client_id as ClientId from b_ippool_details p";
			return " p.id ,p.pool_name as poolName, p.client_id as ClientId,c.display_name as ClientName, " +
					" p.ip_address as ipAddress ,p.status,p.notes from b_ippool_details p left join m_client c on p.client_id = c.id";
		}

		@Override
		public IpPoolManagementData mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			
			Long id=rs.getLong("id");
			String poolName=rs.getString("poolName");
			Long ClientId=rs.getLong("ClientId");
			String clientName=rs.getString("ClientName");
			String ipAddress=rs.getString("ipAddress");	
			String status=rs.getString("status");
			String notes=rs.getString("notes");
			
			
			return new IpPoolManagementData(id, ipAddress, poolName,status, ClientId, clientName, notes);
		}
	}

	@Override
	public Page<IpPoolManagementData> retrieveIpPoolData(SearchSqlQuery searchIpPoolDetails, String tabType) {
		
		// TODO Auto-generated method stub
		context.authenticatedUser();
		IpPoolMapper mapper=new IpPoolMapper();
		
		String sqlSearch = searchIpPoolDetails.getSqlSearch();
	    String extraCriteria = "";
		StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ");
        sqlBuilder.append(mapper.schema());
       
          
        if (tabType!=null ) {
        	
		        	tabType=tabType.trim();
		        	sqlBuilder.append(" where p.status like '"+tabType+"' order by p.id ");
		  
		    	    if (sqlSearch != null) {
		    	    	sqlSearch=sqlSearch.trim();
		    	    	extraCriteria = " and (p.ip_address like '%"+sqlSearch+"%' OR p.pool_name like '%"+sqlSearch+"%') order by p.id";
		    	    }
		            sqlBuilder.append(extraCriteria);
		            
	    }else if (sqlSearch != null) {
    	    	sqlSearch=sqlSearch.trim();
    	    	extraCriteria = " where (p.ip_address like '%"+sqlSearch+"%' OR p.pool_name like '%"+sqlSearch+"%') order by p.id";
    	}else {
    		extraCriteria = " order by p.id ";
    	}
                sqlBuilder.append(extraCriteria);
        
        
        if (searchIpPoolDetails.isLimited()) {
            sqlBuilder.append(" limit ").append(searchIpPoolDetails.getLimit());
        }

        if (searchIpPoolDetails.isOffset()) {
            sqlBuilder.append(" offset ").append(searchIpPoolDetails.getOffset());
        }

		return this.paginationHelper.fetchPage(this.jdbcTemplate, "SELECT FOUND_ROWS()",sqlBuilder.toString(),
                new Object[] {}, mapper);
	
	}
	
	@Override
	public List<String> retrieveIpPoolIDArray(String query) {
		IpAddressPoolingArrayMapper mapper = new IpAddressPoolingArrayMapper();
		String sql = "select " + mapper.schema()+ " and  p.ip_address like '"+ query +"%'";
		return this.jdbcTemplate.query(sql, mapper, new Object[] {});
	}

	private static final class IpAddressPoolingArrayMapper implements RowMapper<String> {

		public String schema() {
		
			return "p.ip_address as ipAddress from b_ippool_details p where p.status='F'";
		}
		
		@Override
		public String mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			
			//Long id=rs.getLong("id");
			String ipAddress=rs.getString("ipAddress");
			/*String poolName=rs.getString("poolName");
			String status=rs.getString("status");
			Long ClientId=rs.getLong("ClientId");*/
			
			return ipAddress;
		}
}

}

