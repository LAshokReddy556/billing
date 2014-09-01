package org.mifosplatform.finance.payments.api;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;
import org.mifosplatform.billing.paymode.data.McodeData;
import org.mifosplatform.billing.paymode.service.PaymodeReadPlatformService;
import org.mifosplatform.billing.selfcare.domain.SelfCareTemporary;
import org.mifosplatform.billing.selfcare.domain.SelfCareTemporaryRepository;
import org.mifosplatform.billing.selfcare.exception.SelfCareTemporaryAlreadyExistException;
import org.mifosplatform.billing.selfcare.exception.SelfCareTemporaryEmailIdNotFoundException;
import org.mifosplatform.commands.domain.CommandWrapper;
import org.mifosplatform.commands.service.CommandWrapperBuilder;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.finance.payments.data.PaymentData;
import org.mifosplatform.finance.payments.exception.DalpayRequestFailureException;
import org.mifosplatform.infrastructure.codes.data.CodeData;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;

@Path("/payments")
@Component
@Scope("singleton")
public class PaymentsApiResource {

	/**
	 * The set of parameters that are supported in response for {@link CodeData}
	 */
	private static final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<String>(
			Arrays.asList("id", "clientId", "paymentDate", "paymentCode",
					"amountPaid", "statmentId", "externalId", "Remarks"));
	private final String resourceNameForPermissions = "GETPAYMENT";

	private final PlatformSecurityContext context;
	private final PaymodeReadPlatformService readPlatformService;
	private final DefaultToApiJsonSerializer<PaymentData> toApiJsonSerializer;
	private final ApiRequestParameterHelper apiRequestParameterHelper;
	private final PortfolioCommandSourceWritePlatformService writePlatformService;
	private final SelfCareTemporaryRepository selfCareTemporaryRepository;

	@Autowired
	public PaymentsApiResource(final PlatformSecurityContext context,final PaymodeReadPlatformService readPlatformService,
			final DefaultToApiJsonSerializer<PaymentData> toApiJsonSerializer,final ApiRequestParameterHelper apiRequestParameterHelper,
			final PortfolioCommandSourceWritePlatformService writePlatformService, final SelfCareTemporaryRepository selfCareTemporaryRepository) {
		this.context = context;
		this.readPlatformService = readPlatformService;
		this.toApiJsonSerializer = toApiJsonSerializer;
		this.apiRequestParameterHelper = apiRequestParameterHelper;
		this.writePlatformService = writePlatformService;
		this.selfCareTemporaryRepository = selfCareTemporaryRepository;
	}

	@POST
	@Path("{clientId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String createPayment(@PathParam("clientId") final Long clientId,	final String apiRequestBodyAsJson) {
		final CommandWrapper commandRequest = new CommandWrapperBuilder().createPayment(clientId).withJson(apiRequestBodyAsJson).build();
		final CommandProcessingResult result = this.writePlatformService.logCommandSource(commandRequest);
		return this.toApiJsonSerializer.serialize(result);
	}

	@GET
	@Path("template")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveDetailsForPayments(@QueryParam("clientId") final Long clientId,@Context final UriInfo uriInfo) {
		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		Collection<McodeData> data = this.readPlatformService.retrievemCodeDetails("Payment Mode");
		PaymentData paymentData=new PaymentData(data);
		final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());
		return this.toApiJsonSerializer.serialize(settings, paymentData,RESPONSE_DATA_PARAMETERS);

	}
	
	@GET
	@Path("{clientId}")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public String retrieveAllDetailsForPayments(@PathParam("clientId") final Long clientId,@Context final UriInfo uriInfo) {
		context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);
		List<PaymentData> paymentData = readPlatformService.retrivePaymentsData(clientId);
		final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());
		return this.toApiJsonSerializer.serialize(settings, paymentData,RESPONSE_DATA_PARAMETERS);

	}
	
	
	
	 @POST
	 @Path("paypal")
	 @Consumes("application/x-www-form-urlencoded")
	 @Produces({ MediaType.APPLICATION_JSON })
	 public String Checkout(@FormParam("txn_id") String txnId,@FormParam("payment_date") Date Date,@FormParam("mc_gross") BigDecimal amount,
			 @FormParam("address_name") String name,@FormParam("payer_email") String payerEmail,
			 @FormParam("transaction_subject") String clientStringId){
	   try {
		   
		  Long clientId= Long.parseLong(clientStringId);
		  SimpleDateFormat daformat=new SimpleDateFormat("dd MMMM yyyy");
		  String date=daformat.format(Date);
		  JsonObject object=new JsonObject();
		  object.addProperty("txn_id", txnId);
		  object.addProperty("dateFormat","dd MMMM yyyy");
		  object.addProperty("locale","en");
		  object.addProperty("paymentDate",date);
		  object.addProperty("amountPaid",amount);
		  object.addProperty("isChequeSelected","no");
		  object.addProperty("receiptNo",txnId);
		  object.addProperty("remarks",payerEmail);
		  object.addProperty("paymentCode",27);
		  
		  final CommandWrapper commandRequest = new CommandWrapperBuilder().createPayment(clientId).withJson(object.toString()).build();
		  final CommandProcessingResult result1 = this.writePlatformService.logCommandSource(commandRequest);
		  return this.toApiJsonSerializer.serialize(result1); 
	        
	  } 
	   catch(Exception e){
	    return e.getMessage();
	   }
	 }
	 
	 @PUT
	 @Path("cancelpayment/{paymentId}")
	 @Consumes({ MediaType.APPLICATION_JSON })
	 @Produces({ MediaType.APPLICATION_JSON })
	 public String cancelPayment(@PathParam("paymentId") final Long paymentId,final String apiRequestBodyAsJson) {
		 
			final CommandWrapper commandRequest = new CommandWrapperBuilder().cancelPayment(paymentId).withJson(apiRequestBodyAsJson).build();
			final CommandProcessingResult result = this.writePlatformService.logCommandSource(commandRequest);
			return this.toApiJsonSerializer.serialize(result);
		}	
	 

	 @POST
	 @Path("dalpay")
	 @Consumes({ MediaType.APPLICATION_JSON })
	 @Produces({ MediaType.TEXT_HTML})
	 public String dalpayCheckout(final String apiRequestBodyAsJson){
		   try {
			   	  JSONObject json= new JSONObject(apiRequestBodyAsJson);
			   	  String orderNumber = json.getString("order_num");
			   	  Long clientId = json.getLong("user1");
			   	  String returnUrl = json.getString("user2");
			   	  String screenName = json.getString("user3");
			   	  String EmailId = json.getString("cust_email");
			   	  String amount = json.getString("total_amount");
			   	  BigDecimal totalAmount = new BigDecimal(amount);
			   	returnUrl = returnUrl.replace("index.html", "index.html#/"+screenName);
			   	  
				if (clientId !=null && clientId > 0) {
					String date = new SimpleDateFormat("dd MMMM yyyy").format(new Date());
					JsonObject object = new JsonObject();

					object.addProperty("txn_id", orderNumber);
					object.addProperty("dateFormat", "dd MMMM yyyy");
					object.addProperty("locale", "en");
					object.addProperty("paymentDate", date);
					object.addProperty("amountPaid", totalAmount);
					object.addProperty("isChequeSelected", "no");
					object.addProperty("receiptNo", orderNumber);
					object.addProperty("remarks", "Updated with Dalpay");
					object.addProperty("paymentCode", 27);

					final CommandWrapper commandRequest = new CommandWrapperBuilder().createPayment(clientId).withJson(object.toString()).build();
					final CommandProcessingResult result = this.writePlatformService.logCommandSource(commandRequest);
					
				}else if(clientId !=null && clientId == 0){
					
					SelfCareTemporary selfCareTemporary = this.selfCareTemporaryRepository.findOneByEmailId(EmailId);
					if(selfCareTemporary != null && selfCareTemporary.getPaymentStatus().equalsIgnoreCase("INACTIVE")){
						
						selfCareTemporary.setPaymentData(json.toString());
						selfCareTemporary.setPaymentStatus("PENDING");
						this.selfCareTemporaryRepository.save(selfCareTemporary);											
						
					}else if(selfCareTemporary != null){				
						throw new SelfCareTemporaryAlreadyExistException(EmailId);					
					}else{					
						throw new SelfCareTemporaryEmailIdNotFoundException(EmailId);					
					}		
				}else{		
					throw new DalpayRequestFailureException(clientId);				
				}
				
				return "<!-- success--> <span>Order Accepted Successfully</span>"
				+ "<br>"
				+ "<a href='"+ returnUrl +"'>"
				+ "<strong>CLICK HERE</strong> to return to your account</a>";
			   	  
				  
		  } 
		   catch(Exception e){
		    return e.getMessage();
		   }
		 }
	 
	 @POST
	 @Path("authorizeNet")
	 @Consumes("application/x-www-form-urlencoded")
	 @Produces({ MediaType.TEXT_HTML})
	 public String authorizeNetCheckout(@FormParam("x_email") String email,
			 @FormParam("x_invoice_num") String invoiceNumber){
	   try {
		   	  System.out.println(invoiceNumber);
			return "<!-- success--> <span>Order Accepted Successfully</span>";
	  } 
	   catch(Exception e){
	    return e.getMessage();
	   }
	 }
	 
	 @POST
	 @Path("paypalEnquirey/{clientId}")
	 @Consumes({ MediaType.APPLICATION_JSON })
	 @Produces({ MediaType.APPLICATION_JSON })
	 public String paypalEnquireyPayment(@PathParam("clientId") final Long clientId,final String apiRequestBodyAsJson) {
		 
			final CommandWrapper commandRequest = new CommandWrapperBuilder().PaypalPayment(clientId).withJson(apiRequestBodyAsJson).build();
			final CommandProcessingResult result = this.writePlatformService.logCommandSource(commandRequest);
			return this.toApiJsonSerializer.serialize(result);
		}
}
