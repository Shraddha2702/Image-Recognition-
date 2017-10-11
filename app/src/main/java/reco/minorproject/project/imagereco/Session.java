package reco.minorproject.project.imagereco;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by SHRADDHA on 05-10-2017.
 */

public class Session {
    Context myContext;
    Boolean isLoggedIn=false;
    SharedPreferences pref;
    SharedPreferences.Editor edit;

    Session(Context myContext){
        this.myContext=myContext;
        pref=myContext.getSharedPreferences("Blind",0);
        edit= pref.edit();
    }

    public Boolean isBlind() {

        return pref.getBoolean("isBlind",isLoggedIn);
    }

    public void setBlind(Boolean loggedIn) {
        isLoggedIn = loggedIn;
        edit.putBoolean("isBlind",loggedIn);
        edit.commit();
    }

    public void deleteblind(){
        edit.clear();
        edit.commit();
    }
}