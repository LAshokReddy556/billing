package org.mifosplatform.organisation.ippool.service;

import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.ippool.data.IpGeneration;
import org.mifosplatform.organisation.ippool.domain.IpPoolManagementDetail;
import org.mifosplatform.organisation.ippool.domain.IpPoolManagementJpaRepository;
import org.mifosplatform.organisation.ippool.serialization.IpPoolManagementCommandFromApiJsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IpPoolManagementWritePlatformServiceImpl implements IpPoolManagementWritePlatformService{
	
	private final PlatformSecurityContext context; 
	private final IpPoolManagementCommandFromApiJsonDeserializer apiJsonDeserializer;
	private final IpPoolManagementJpaRepository ipPoolManagementJpaRepository;
	private final IpPoolManagementReadPlatformService ipPoolManagementReadPlatformService;
	
	
	@Autowired
	public IpPoolManagementWritePlatformServiceImpl(final PlatformSecurityContext context,
			final IpPoolManagementJpaRepository ipPoolManagementJpaRepository,
			final IpPoolManagementCommandFromApiJsonDeserializer apiJsonDeserializer,
			final IpPoolManagementReadPlatformService ipPoolManagementReadPlatformService) {	
		
		this.context = context;
		this.apiJsonDeserializer=apiJsonDeserializer;
		this.ipPoolManagementJpaRepository=ipPoolManagementJpaRepository;
		this.ipPoolManagementReadPlatformService=ipPoolManagementReadPlatformService;
		
	}

	@Transactional
	@Override
	public CommandProcessingResult createIpPoolManagement(JsonCommand command) {

		
		try {
			context.authenticatedUser();
			this.apiJsonDeserializer.validateForCreate(command.json());
			String ipAddress=command.stringValueOfParameterNamed("ipAddress");
			String subnet=command.stringValueOfParameterNamed("subnet");
			String ipPoolDescription=command.stringValueOfParameterNamed("ipPoolDescription");
			String ipData=ipAddress+"/"+subnet;
			IpGeneration util=new IpGeneration(ipData,this.ipPoolManagementReadPlatformService);
			String[] data=util.getInfo().getAllAddresses();
			
			for(int i=0;i<data.length;i++){
				IpPoolManagementDetail ipPoolManagementDetail= new IpPoolManagementDetail(data[i],ipPoolDescription);
				this.ipPoolManagementJpaRepository.save(ipPoolManagementDetail);
			}
		
			return new CommandProcessingResultBuilder().build();
			
		}catch(DataIntegrityViolationException dve){
			return CommandProcessingResult.empty();
		}catch (Exception e) {
			return null;
		}
		
	}

	@Transactional
	@Override
	public CommandProcessingResult editIpPoolManagement(JsonCommand command) {
		
		try {
			context.authenticatedUser();
			this.apiJsonDeserializer.validateForUpdate(command.json());
			
			String statusType = command.stringValueOfParameterNamed("statusType");
			String notes = command.stringValueOfParameterNamed("notes");
			
			IpPoolManagementDetail ipPoolManagementDetail = this.ipPoolManagementJpaRepository.findOne(command.entityId());
			ipPoolManagementDetail.setStatus(statusType.trim().charAt(0));
			ipPoolManagementDetail.setNotes(notes);
			
			this.ipPoolManagementJpaRepository.save(ipPoolManagementDetail);
			
			return new CommandProcessingResultBuilder().build();

		} catch (DataIntegrityViolationException dve) {
			return CommandProcessingResult.empty();
		} catch (Exception e) {
			return null;
		}

	}

}
