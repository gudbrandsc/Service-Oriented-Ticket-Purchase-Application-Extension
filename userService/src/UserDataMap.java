import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Gudbrand Schistad
 * Class containing a Concurrent HashMap.
 */
public class UserDataMap {
    private ConcurrentHashMap<Integer, User> userData;

    /** Constructor */
    public UserDataMap() {
        this.userData =  new ConcurrentHashMap<Integer, User>();
    }

    /** Method used to add a user to the map
     * @param userId Id of the user (key)
     * @param user User object
     */
    public synchronized void addUser(int userId, User user){
        if(!checkIfUserExist(userId)) {
            this.userData.put(userId, user);
        }
    }

    /**
     * Method to get a user from the map.
     * @param userId Id of the user
     * @return User object
     */
    public User getUser(int userId) {
        return this.userData.get(userId);
    }

    /**
     * Method used to check if the map contains the userid
     * @param userId Id of the user(key)
     * @return True if user exist, else false.
     */
    public boolean checkIfUserExist(int userId){
        return this.userData.containsKey(userId);
    }

    public JSONObject buildMapObject(){
        JSONObject obj = new JSONObject();
        JSONArray usersInfo = new JSONArray();
        for(User user : userData.values()){
            JSONObject userObj = new JSONObject();
            userObj.put("userid",user.getUserid());
            userObj.put("username", user.getUsername());
            userObj.put("tickets", user.getTicketArray());
            usersInfo.add(userObj);
        }
        obj.put("userdata", usersInfo);
        return obj;
    }
    public void updateuserDataMap(ConcurrentHashMap<Integer, User> newMap){
        this.userData = newMap;
    }
}

