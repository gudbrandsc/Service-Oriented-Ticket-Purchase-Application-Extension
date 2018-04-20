import unittest, urllib2
import json as simplejson
import json
import sys
import threading
import uuid





BASE_URL = 'http://' + sys.argv[1] +  ':' + sys.argv[2]

"""Method used to generate a random string.
Source: https://stackoverflow.com/questions/2257441/random-string-generation-with-upper-case-letters-and-digits-in-python"""
def my_random_string(string_length=10):
    random = str(uuid.uuid4()) 
    random = random.replace("-","") 
    return random[0:string_length]


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
 		print 'Failed to purchase tickets: ' + str(e.code)
	

#Transfere n tickets for eventid e from user a to user b
def transfere_tickets(eventid, userid, number_of_tickets, targetuser):
	print'---- Transfere Tickets ----'
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
		print  'StatusCode: ' + str(response.getcode())
		print 'Transfered '  + str(number_of_tickets) + ' tickets from userid ' + str(userid) + ' to user ' + str(targetuser) + '. Eventid = ' + str(eventid) + '\n'
 	except urllib2.HTTPError as e:
 		print 'Failed to transfer tickets: ' + str(e.code)
	

if __name__ == '__main__':
	print  '\n'
	username1 = 'TestUser1'
	event_name = my_random_string(10);
	number_of_tickets = 10

	#POST /users/create
	userid = create_user(username1)

	
	#POST /events/create
	eventid = create_event(userid, event_name, number_of_tickets)
	

	#POST /events/{eventid}/purchase/{userid} 
	purchase_success = purchase_tickets(eventid, userid, 5)

	tickets_purchased = number_of_tickets; # tickets_purchased = 99
	targetuser = create_user("targetuser")
	transfer_success = transfere_tickets(eventid, userid, 5, targetuser)

















