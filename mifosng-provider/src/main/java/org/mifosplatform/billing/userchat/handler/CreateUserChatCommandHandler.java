package org.mifosplatform.billing.userchat.handler;

import org.mifosplatform.billing.userchat.service.UserChatWriteplatformService;
import org.mifosplatform.commands.handler.NewCommandSourceHandler;
import org.mifosplatform.infrastructure.core.api.JsonCommand;
import org.mifosplatform.infrastructure.core.data.CommandProcessingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreateUserChatCommandHandler  implements NewCommandSourceHandler {

	private final UserChatWriteplatformService writePlatformService;
	  
	  @Autowired
	    public CreateUserChatCommandHandler(UserChatWriteplatformService writePlatformService) {
	        this.writePlatformService = writePlatformService;
	       
	    }

	@Override
	public CommandProcessingResult processCommand(JsonCommand command) {
		
		return writePlatformService.createUserChat(command);
	}
	

}