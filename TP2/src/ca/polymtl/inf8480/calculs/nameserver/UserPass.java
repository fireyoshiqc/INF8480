package ca.polymtl.inf8480.calculs.nameserver;

public class UserPass {
    private String salt;
    private String hashedPass;

    public UserPass(String salt, String hashedPass) {
        this.salt = salt;
        this.hashedPass = hashedPass;
    }

    public String getSalt(){
        return this.salt;
    }

    public String getHashedPass(){
        return this.hashedPass;
    }
}
