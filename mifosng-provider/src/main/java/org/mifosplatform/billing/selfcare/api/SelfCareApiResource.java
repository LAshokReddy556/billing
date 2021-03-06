package org.mifosplatform.billing.selfcare.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.mifosplatform.billing.paymode.service.PaymodeReadPlatformService;
import org.mifosplatform.billing.selfcare.data.SelfCareData;
import org.mifosplatform.billing.selfcare.service.SelfCareReadPlatformService;
import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.crm.ticketmaster.data.TicketMasterData;
import org.mifosplatform.crm.ticketmaster.service.TicketMasterReadPlatformService;
import org.mifosplatform.finance.billingmaster.service.BillMasterReadPlatformService;
import org.mifosplatform.finance.clientbalance.data.ClientBalanceData;
import org.mifosplatform.finance.clientbalance.service.ClientBalanceReadPlatformService;
import org.mifosplatform.finance.financialtransaction.data.FinancialTransactionsData;
import org.mifosplatform.finance.payments.data.PaymentData;
import org.mifosplatform.infrastructure.configuration.domain.ConfigurationConstants;
import org.mifosplatform.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.mifosplatform.infrastructure.configuration.domain.GlobalConfigurationRepository;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.exception.PlatformDataIntegrityException;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.organisation.address.data.AddressData;
import org.mifosplatform.organisation.address.service.AddressReadPlatformService;
import org.mifosplatform.portfolio.client.data.ClientData;
import org.mifosplatform.portfolio.client.service.ClientReadPlatformService;
import org.mifosplatform.portfolio.order.data.OrderData;
import org.mifosplatform.portfolio.order.service.OrderReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.stereotype.Component;

@Path("selfcare")
@Component
@Scope("singleton")
public class SelfCareApiResource {

	
	private final Set<String> supportedParameters = new HashSet<String>(Arrays.asList(""));
	private PlatformSecurityContext context;
	private final String resourceNameForPermissions = "SELFCARE";
	private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
	private final DefaultToApiJsonSerializer<SelfCareData> toApiJsonSerializerForItem;
	private final SelfCareReadPlatformService selfCareReadPlatformService;
	private final ClientReadPlatformService clientReadPlatformService;
	private final AddressReadPlatformService addressReadPlatformService;
	private final ClientBalanceReadPlatformService clientBalanceReadPlatformService;
	private final OrderReadPlatformService orderReadPlatformService;
	private final BillMasterReadPlatformService billMasterReadPlatformService;
	private final PaymodeReadPlatformService paymentReadPlatformService;
	private final TicketMasterReadPlatformService ticketMasterReadPlatformService;
	private final GlobalConfigurationRepository configurationRepository;
	
	
	@Autowired
	public SelfCareApiResource(final PlatformSecurityContext context,final PortfolioCommandSourceWritePlatformService commandSourceWritePlatformService, 
			final DefaultToApiJsonSerializer<SelfCareData> toApiJsonSerializerForItem,final SelfCareReadPlatformService selfCareReadPlatformService, 
			final PaymodeReadPlatformService paymentReadPlatformService, final AddressReadPlatformService addressReadPlatformService, 
			final ClientBalanceReadPlatformService balanceReadPlatformService, final ClientReadPlatformService clientReadPlatformService, 
			final OrderReadPlatformService  orderReadPlatformService, final BillMasterReadPlatformService billMasterReadPlatformService,
			final TicketMasterReadPlatformService ticketMasterReadPlatformService,final GlobalConfigurationRepository configurationRepository) {
		
				this.context = context;
				this.commandsSourceWritePlatformService = commandSourceWritePlatformService;
				this.toApiJsonSerializerForItem = toApiJsonSerializerForItem;
				this.selfCareReadPlatformService = selfCareReadPlatformService;
				this.paymentReadPlatformService = paymentReadPlatformService;
				this.addressReadPlatformService = addressReadPlatformService;
				this.clientBalanceReadPlatformService = balanceReadPlatformService;
				this.clientReadPlatformService = clientReadPlatformService;
				this.orderReadPlatformService = orderReadPlatformService;
				this.billMasterReadPlatformService = billMasterReadPlatformService;
				this.ticketMasterReadPlatformService = ticketMasterReadPlatformService;
				this.configurationRepository=configurationRepository;
	}
	
	
	
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String createSelfCareClient(final String jsonRequestBody) {
		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final CommandWrapper commandRequest = new CommandWrapperBuilder().createSelfCare().withJson(jsonRequestBody).build();
		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
	    return this.toApiJsonSerializerForItem.serialize(result);	
	}
	


	@Path("password")
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String createSelfCareClientUDPassword(final String jsonRequestBody) {
		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		final CommandWrapper commandRequest = new CommandWrapperBuilder().createSelfCareUDP().withJson(jsonRequestBody).build();

		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);	 
	    return this.toApiJsonSerializerForItem.serialize(result);	
	}	
	
	@Path("/login")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String logIn(@QueryParam("username") final String username, @QueryParam("password") final String password){
        
		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
        
        SelfCareData careData = new SelfCareData();
        try{
        final SelfCareData selfcare = this.selfCareReadPlatformService.login(username, password);
        
      /*  if(selfcare == null && selfcare.getClientId() == null){
        	  MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
        	   throw new BadCredentialsException(messages.getMessage(
                       "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }*/
        
        if(selfcare != null && selfcare.getPassword().equals(password) && selfcare.getClientId()>0){
        	Long clientId = selfcare.getClientId();
        	careData.setClientId(clientId);
            ClientData clientsData = this.clientReadPlatformService.retrieveOne(clientId);
            ClientBalanceData balanceData = this.clientBalanceReadPlatformService.retrieveBalance(clientId);
            List<AddressData> addressData = this.addressReadPlatformService.retrieveAddressDetails(clientId);
            final List<OrderData> clientOrdersData = this.orderReadPlatformService.retrieveClientOrderDetails(clientId);
            final List<FinancialTransactionsData> statementsData = this.billMasterReadPlatformService.retrieveStatments(clientId);
            List<PaymentData> paymentsData = paymentReadPlatformService.retrivePaymentsData(clientId);
            final List<TicketMasterData> ticketMastersData = this.ticketMasterReadPlatformService.retrieveClientTicketDetails(clientId);
            
            careData.setDetails(clientsData,balanceData,addressData,clientOrdersData,statementsData,paymentsData,ticketMastersData);
            
            //adding Is_paypal Global Data by Ashok
            GlobalConfigurationProperty paypalConfigData=this.configurationRepository.findOneByName(ConfigurationConstants.CONFIG_PROPERTY_IS_PAYPAL_CHECK);
            careData.setPaypalConfigData(paypalConfigData);
        	
        }else{
        	MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
      	    throw new BadCredentialsException(messages.getMessage(
                     "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
        
     /*   SelfCare selfCare=this.selfCareRepository.findOneByClientId(clientId);
        if(selfCare.getStatus().equalsIgnoreCase("ACTIVE")){
        	throw new ClientStatusException(clientId);
        }else{
        	selfCare.setStatus("ACTIVE");
        	this.selfCareRepository.saveAndFlush(selfCare);
        }*/
        
        
        
        }catch(EmptyResultDataAccessException e){
        	throw new PlatformDataIntegrityException("result.set.is.null","result.set.is.null","result.set.is.null");
        }
        return this.toApiJsonSerializerForItem.serialize(careData);
        
	}
	
	  
    @PUT
    @Path("status/{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateClientStatus(@PathParam("clientId")Long clientId,final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateClientStatus(clientId) //
                .withJson(apiRequestBodyAsJson) //
                .build(); //

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializerForItem.serialize(result);
    }
    
    @POST
    @Path("register")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String registerSelfCare(final String jsonRequestBody) {

		final CommandWrapper commandRequest = new CommandWrapperBuilder().registerSelfCareRegister().withJson(jsonRequestBody).build();
		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
	    return this.toApiJsonSerializerForItem.serialize(result);	
	    
	}
    
    @PUT
    @Path("register")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String SelfCareEmailVerication(final String jsonRequestBody) {

		final CommandWrapper commandRequest = new CommandWrapperBuilder().SelfCareEmailVerification().withJson(jsonRequestBody).build();
		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
	    return this.toApiJsonSerializerForItem.serialize(result);	
	    
	}
    
    @POST
    @Path("forgotpassword")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String createNewSelfCarePassword(final String jsonRequestBody) {

		final CommandWrapper commandRequest = new CommandWrapperBuilder().createNewSelfCarePassword().withJson(jsonRequestBody).build();
		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
	    return this.toApiJsonSerializerForItem.serialize(result);	
	    
	}
    
    @PUT
    @Path("changepassword")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String selfCareChangePassword(final String jsonRequestBody) {

		final CommandWrapper commandRequest = new CommandWrapperBuilder().updateSelfcarePassword().withJson(jsonRequestBody).build();
		final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
	    return this.toApiJsonSerializerForItem.serialize(result);	
	    
	}
	
}
