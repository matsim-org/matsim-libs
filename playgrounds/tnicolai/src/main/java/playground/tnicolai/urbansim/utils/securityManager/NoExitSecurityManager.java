package playground.tnicolai.urbansim.utils.securityManager;

import java.security.Permission;

public class NoExitSecurityManager extends SecurityManager {
	
    public void checkPermission(Permission perm) 
    {
        // allow anything.
    }
    
    public void checkPermission(Permission perm, Object context) 
    {
        // allow anything.
    }
    
    @Override
    public void checkExit(int status) 
    {
            super.checkExit(status);
            throw new ExitException(status);
    }
}


