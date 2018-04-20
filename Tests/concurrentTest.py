import unittest, urllib2
import json as simplejson
import json
import sys
import threading
import uuid


BASE_URL = 'http://' + sys.argv[1] +  ':' + sys.argv[2]


def my_random_string(string_length=10):
    """Returns a random string of length string_length."""
    random = str(uuid.uuid4()) # Convert UUID format to a Python string.
    random = random.replace("-","") # Remove the UUID '-'.
    return random[0:string_length] # Return the random string.


#Sends post request with username to create a user
def create_user(username):
	print'---- Create User ----'
	data = {'username': username}
	url = ('%s/users/create' % (BASE_URL))
	req = urllib2.Request(url)
	req.add_header('Content-Type', 'application/json')
	try:
		response = urllib2.urlopen(req, json.dumps(data))
		print  'StatusCode: ' + str(response.getcode())
		data = simplejson.load(response)
		userid = data["userid"]
		print 'Created user with ID: '  + str(userid)  + '\n'
		return userid
 	except urllib2.HTTPError as e:
 		print 'Failed to create user ' + str(e.code)
	
	

#Sends post request to create a event, and returns eventid
def create_event(userid, event_name, number_of_tickets):
	print'---- Create Event ----'
	data = {
	'userid': userid,
	'eventname': event_name,
	'numtickets': number_of_tickets}

	url = ('%s/events/create' % (BASE_URL))
	req = urllib2.Request(url)
	req.add_header('Content-Type', 'application/json')
	try:
		response = urllib2.urlopen(req, json.dumps(data))
		print  'StatusCode: ' + str(response.getcode())
		data = simplejson.load(response)
		eventid = data["eventid"]
		print 'Created event with ID: '  + str(eventid) + '\n'
		return eventid
 	except urllib2.HTTPError as e:
 		print 'Failed to create event: ' + str(e.code)
	
#Sends post request to purchase n tickets for event x to user y
def purchase_tickets(eventid, userid, number_of_tickets):
	print'---- Purchase Tickets ----'
	data = {'tickets': number_of_tickets}
	url = ('%s/events/%s/purchase/%s' % (BASE_URL, eventid, userid))

	req = urllib2.Request(url)
	req.add_header('Content-Type', 'application/json')
	try:
		response = urllib2.urlopen(req, json.dumps(data))
		print  'StatusCode: ' + str(response.getcode())
		print 'Purchased '  + str(number_of_tickets) + ' tickets for eventid ' + str(eventid) + ' to userid ' + str(userid) + '\n'
 	except urllib2.HTTPError as e:
 		print 'Failed to purchase ticket(s): ' + str(e.code)
	
#Transfere n tickets for eventid e from user a to user b
def transfere_tickets(eventid, userid, number_of_tickets, targetuser):
	data = {
	"eventid": eventid,
	"tickets": number_of_tickets,
	"targetuser": targetuser
	}

	url = ('%s/users/%s/tickets/transfer' % (BASE_URL, userid))
	req = urllib2.Request(url)
	req.add_header('Content-Type', 'application/json')
	try:
		response = urllib2.urlopen(req, json.dumps(data))
 	except urllib2.HTTPError as e:
 		print 'Failed to transfer ticket(s): ' + str(e.code)
	

def worker(userid, targetuserId, eventid):
	transfer_success = transfere_tickets(eventid, userid, 1, targetuserId)



""" Main method that creates a user, and a event. 
Then purchase 100 tickets from the created event, to the created user.
Then creates a second user and sends a 105 transfere requests of 1 ticket from user 1 to user 2. """
if __name__ == '__main__':
	username = 'TestUser1'
	targetuser = 'targetuser'
	event_name = my_random_string(10);
	number_of_tickets = 100

	userid = create_user(username)
	targetuserId = create_user(targetuser)

	eventid = create_event(userid, event_name, number_of_tickets)
	purchase_success = purchase_tickets(eventid, userid, 100)


	threads = []
	print 'Starting 105 requests to transfere 1 ticket from ' + str(userid) + ' to ' + str(targetuserId) 
	count = 0
	for i in range(105):
		t = threading.Thread(target=worker(userid, targetuserId, eventid))
    	t.start()
    
