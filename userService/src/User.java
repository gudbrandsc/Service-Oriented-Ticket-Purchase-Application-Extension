import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Author Gudbrand Schistad
 * Class used to create a user object, that is stored in the user datamap.
 */
public class User {
    private int userid;
    private String username;
    JSONArray jsonArray = null;

    /*** constructor*/
    public User(int userid, String username) {
        this.userid = userid;
        this.username = username;
        this.jsonArray = new JSONArray();
    }

    /**
     * Validates that the user has a event with the param eventid,
     * and that the number of tickets param is not greater than the number the user has
     * @param eventid Id of the event
     * @param numTickets number of tickets
     */
    public boolean validateNumTickets(int eventid, int numTickets) {
        int count = 0;
        Iterator<JSONObject> it = jsonArray.iterator();

        while(it.hasNext()){
            JSONObject obj = it.next();
            int value = (int) Long.parseLong(obj.get("eventid").toString());
            if(value == eventid){
                count++;
            }
        }
        return count >= numTickets;
    }

    /** Get method to retrieve the username
     * @return username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Method that adds tickets to an event.
     * Checks if the user already has any tickets for the event id.
     * If the user do then add the number of tickets to that event.
     * else put eventid and number of tickets.
     * @param eventid Id of the event
     * @param numTickets Number of tickets purchased/transferred
     */
    public void addTickets(int eventid, int numTickets) {
        for(int i = 0; i < numTickets; i++){
            JSONObject obj = new JSONObject();
            obj.put("eventid", eventid);
            jsonArray.add(obj);
        }
    }

    /**
     * Method that removes tickets from a event.
     * Uses validateNumTickets method to make sure that the number of tickets removed is
     * not greater than the number of tickets the user holds.
     * If the number of tickets is equal to zero then remove the event from the eventlist
     * @param eventid Id of the event
     * @param numTickets Number of tickets to remove
     */
    public void removeTickets(int eventid, int numTickets) {
        int count = 0;
        boolean removedAll = false;
        JSONArray newArray = new JSONArray();
        Iterator<JSONObject> it = jsonArray.iterator();

        while(it.hasNext()){
            if(count == numTickets){
                removedAll = true;
            }
            JSONObject obj = it.next();
            int value = (int) Long.parseLong(obj.get("eventid").toString());

            if(value == eventid && !removedAll){

            }else{
                newArray.add(obj);
            }
            count++;
        }
        jsonArray.clear();
        jsonArray.addAll(newArray);
    }

    /**
     * Get method the get the events map
     * @return User hashmap of all events
     */
    public JSONArray getEvents() {
        return this.jsonArray;
    }

    /**
     * Method to get the number of elements in the hashmap
     * @return The size of events map
     */
    public int getNumEventsSize() {
        return this.jsonArray.size();
    }

}
