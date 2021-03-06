package org.mifosplatform.crm.ticketmaster.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import org.mifosplatform.crm.ticketmaster.command.TicketMasterCommand;
import org.mifosplatform.crm.ticketmaster.domain.TicketDetail;
import org.mifosplatform.crm.ticketmaster.domain.TicketDetailsRepository;
import org.mifosplatform.crm.ticketmaster.domain.TicketMaster;
import org.mifosplatform.crm.ticketmaster.domain.TicketMasterRepository;
import org.mifosplatform.crm.ticketmaster.serialization.TicketMasterCloseFromApiJsonDeserializer;
import org.mifosplatform.crm.ticketmaster.serialization.TicketMasterFromApiJsonDeserializer;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResultBuilder;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.core.service.FileUtils;
import org.mifosplatform.infrastructure.documentmanagement.command.DocumentCommand;
import org.mifosplatform.infrastructure.documentmanagement.exception.DocumentManagementException;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.transactionhistory.service.TransactionHistoryWritePlatformService;
import org.mifosplatform.useradministration.domain.AppUser;
import org.mifosplatform.workflow.eventaction.data.ActionDetaislData;
import org.mifosplatform.workflow.eventaction.service.ActionDetailsReadPlatformService;
import org.mifosplatform.workflow.eventaction.service.ActiondetailsWritePlatformService;
import org.mifosplatform.workflow.eventaction.service.EventActionConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
public class TicketMasterWritePlatformServiceImpl implements TicketMasterWritePlatformService{
	
	private PlatformSecurityContext context;
	private TicketMasterRepository repository;
	private TicketDetailsRepository ticketDetailsRepository;
	private TicketMasterFromApiJsonDeserializer fromApiJsonDeserializer;
	private TicketMasterCloseFromApiJsonDeserializer closeFromApiJsonDeserializer;
	private TicketMasterRepository ticketMasterRepository;
	private TicketDetailsRepository detailsRepository;
	private final TransactionHistoryWritePlatformService transactionHistoryWritePlatformService;
	private final ActionDetailsReadPlatformService actionDetailsReadPlatformService; 
	private final ActiondetailsWritePlatformService actiondetailsWritePlatformService;
	
	@Autowired
	public TicketMasterWritePlatformServiceImpl(final PlatformSecurityContext context,
			final TicketMasterRepository repository,final TicketDetailsRepository ticketDetailsRepository, 
			final TicketMasterFromApiJsonDeserializer fromApiJsonDeserializer,final TicketMasterRepository ticketMasterRepository,
			final TicketMasterCloseFromApiJsonDeserializer closeFromApiJsonDeserializer,
			final TransactionHistoryWritePlatformService transactionHistoryWritePlatformService,TicketDetailsRepository detailsRepository,
			final ActionDetailsReadPlatformService actionDetailsReadPlatformService,
			final ActiondetailsWritePlatformService actiondetailsWritePlatformService) {
		this.context = context;
		this.repository = repository;
		this.ticketDetailsRepository=ticketDetailsRepository;
		this.fromApiJsonDeserializer = fromApiJsonDeserializer;
		this.ticketMasterRepository = ticketMasterRepository;
		this.closeFromApiJsonDeserializer = closeFromApiJsonDeserializer;
		this.detailsRepository = detailsRepository;
		this.transactionHistoryWritePlatformService = transactionHistoryWritePlatformService;
		this.actionDetailsReadPlatformService=actionDetailsReadPlatformService;
		this.actiondetailsWritePlatformService=actiondetailsWritePlatformService;
	}

	private void handleDataIntegrityIssues(TicketMasterCommand command,
			DataIntegrityViolationException dve) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long upDateTicketDetails(
			TicketMasterCommand ticketMasterCommand,
			DocumentCommand documentCommand, Long ticketId,InputStream inputStream) {
		
	 	try {
		 String fileUploadLocation = FileUtils.generateFileParentDirectory(documentCommand.getParentEntityType(),
                 documentCommand.getParentEntityId());

         /** Recursively create the directory if it does not exist **/
         if (!new File(fileUploadLocation).isDirectory()) {
             new File(fileUploadLocation).mkdirs();
         }
         String fileLocation=null;
         if(documentCommand.getFileName()!=null){
          fileLocation = FileUtils.saveToFileSystem(inputStream, fileUploadLocation, documentCommand.getFileName());
         }
         Long createdbyId = context.authenticatedUser().getId();
         TicketDetail detail=new TicketDetail(ticketId,ticketMasterCommand.getComments(),fileLocation,ticketMasterCommand.getAssignedTo(),createdbyId);
         /*TicketMaster master = new TicketMaster(ticketMasterCommand.getStatusCode(), ticketMasterCommand.getAssignedTo());*/
         TicketMaster ticketMaster= this.ticketMasterRepository.findOne(ticketId);
         ticketMaster.updateTicket(ticketMasterCommand);
         this.ticketMasterRepository.save(ticketMaster);
         this.ticketDetailsRepository.save(detail);
         transactionHistoryWritePlatformService.saveTransactionHistory(ticketMaster.getClientId(), "UpdateTicketDetails", ticketMaster.getCreatedDate(),
					"Comments:"+ticketMaster.getDescription(),"AssignedTo:"+ticketMaster.getAssignedTo(),"TicketMasterID:"+ticketMaster.getId());
         
         List<ActionDetaislData> actionDetaislDatas=this.actionDetailsReadPlatformService.retrieveActionDetails(EventActionConstants.EVENT_EDIT_TICKET);
  		 if(actionDetaislDatas.size() != 0){
  			this.actiondetailsWritePlatformService.AddNewActions(actionDetaislDatas,ticketMaster.getClientId(), ticketMaster.getId().toString());
  		 }
         return detail.getId();

	 	}
catch (DataIntegrityViolationException dve) {
		handleDataIntegrityIssues(ticketMasterCommand, dve);
		return Long.valueOf(-1);
	
		
		
		
		
		
	} catch (IOException e) {
         throw new DocumentManagementException(documentCommand.getName());
}
		

	
	
	}

	@Override
	public CommandProcessingResult closeTicket( final JsonCommand command) {
		try {
			this.context.authenticatedUser();
			
			this.closeFromApiJsonDeserializer.validateForClose(command.json());
			
			TicketMaster ticketMaster=this.repository.findOne(command.entityId());
			
			if (!ticketMaster.getStatus().equalsIgnoreCase("CLOSED")) {
				ticketMaster.closeTicket(command,this.context.authenticatedUser().getId());
				this.repository.save(ticketMaster);
				transactionHistoryWritePlatformService.saveTransactionHistory(ticketMaster.getClientId(), "TicketClose", ticketMaster.getClosedDate(),
						"Status:"+ticketMaster.getStatus(),"ResolutionDescription:"+ticketMaster.getResolutionDescription(),"TicketMasterID:"+ticketMaster.getId());
				
				List<ActionDetaislData> actionDetaislDatas=this.actionDetailsReadPlatformService.retrieveActionDetails(EventActionConstants.EVENT_CLOSE_TICKET);
		  		 if(actionDetaislDatas.size() != 0){
		  			this.actiondetailsWritePlatformService.AddNewActions(actionDetaislDatas,ticketMaster.getClientId(), ticketMaster.getId().toString());
		  		 }
				
			} else {
				
			}
		}catch (DataIntegrityViolationException dve) {
			handleDataIntegrityIssuesforJson(command, dve);
		}
		return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(command.entityId()).build();
	}

	private void handleDataIntegrityIssuesforJson(JsonCommand command,
			DataIntegrityViolationException dve) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String retrieveTicketProblems(Long ticketId) {
		try {
		TicketMaster master=this.repository.findOne(ticketId);
		String description=master.getDescription();
		return description;
		
		}catch (DataIntegrityViolationException dve) {
			handleDataIntegrityIssues(null, dve);
			return "";
				}
	}

	@Transactional
	@Override
	public CommandProcessingResult createTicketMaster(JsonCommand command) {
		 try {
			 Long created=null;
			 SecurityContext context = SecurityContextHolder.getContext();
	        	if (context.getAuthentication() != null) {
	        		AppUser appUser=this.context.authenticatedUser();
	        		 created=appUser.getId();
	        	}else{
	        		created=new Long(0);
	        	}	 
		this.fromApiJsonDeserializer.validateForCreate(command.json());
		final TicketMaster ticketMaster = TicketMaster.fromJson(command);
		//Long created = context.authenticatedUser().getId();
		ticketMaster.setCreatedbyId(created);
		this.repository.saveAndFlush(ticketMaster);
		final TicketDetail details = TicketDetail.fromJson(command);
		details.setTicketId(ticketMaster.getId());
		details.setCreatedbyId(created);
		this.detailsRepository.saveAndFlush(details);
		transactionHistoryWritePlatformService.saveTransactionHistory(ticketMaster.getClientId(), "Ticket", ticketMaster.getTicketDate(),"Description:"+ticketMaster.getDescription(),
			"Priority:"+ticketMaster.getPriority(),"AssignedTo:"+ticketMaster.getAssignedTo(),"Source:"+ticketMaster.getSource(),"TicketMasterID:"+ticketMaster.getId());
		
		
		List<ActionDetaislData> actionDetaislDatas=this.actionDetailsReadPlatformService.retrieveActionDetails(EventActionConstants.EVENT_CREATE_TICKET);
		if(!actionDetaislDatas.isEmpty()){
			this.actiondetailsWritePlatformService.AddNewActions(actionDetaislDatas,command.getClientId(), ticketMaster.getId().toString());
		}
		
		return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(ticketMaster.getId()).build();
	} catch (DataIntegrityViolationException dve) {
		/*handleDataIntegrityIssues(command, dve);*/
		return new CommandProcessingResult(Long.valueOf(-1));
	} catch (ParseException e) {
		throw new PlatformDataIntegrityException("invalid.date.format", "invalid.date.format", "ticketDate","invalid.date.format");
		
	}
	}
}

