package osr.adapter;


import de.fau.osr.gui.Authentication.Login;

/**
 * @author Gayathery
 *
 */
public class PluginSPICEAuthenticationAdaptor {

    /**
     * This method sets the username and password for the database using the Login class.
     * @param username
     * @param password
     * @return
     */
    public boolean authenticate(String username, String password){
      try{
        if(Login.authenticate(username, password))
            return true;
       
      }catch(Exception e){
          e.printStackTrace();
      }
      return false;
    }
}
