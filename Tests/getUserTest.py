import unittest, urllib2
import json as simplejson
import json
import sys
import threading
import uuid





BASE_URL = 'http://' + sys.argv[1] +  ':' + sys.argv[2]
def verify_new_user():

	url = ('%s/users/%s' % (BASE_URL, sys.argv[3]))
	req = urllib2.Request(url)
	response = urllib2.urlopen(req)

	if(response.getcode() == 200):

		data = simplejson.load(response)
		print (json.dumps(data, indent=2, sort_keys=True))


if __name__ == '__main__':

	verify_new_user()
	

















