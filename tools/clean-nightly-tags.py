#!/usr/bin/python3
# Script responsible for removing extra tags of nightly images
# QUAY_ACCESS_TOKEN is needed to set as environment variable before executing script
# The access token is used for authentication against the quay api.

import os
import json
import requests
from dateutil.relativedelta import *
from dateutil.easter import *
from dateutil.rrule import *
from dateutil.parser import *   
from datetime import *
import argparse


try:
    QUAY_ACCESS_TOKEN = os.environ['QUAY_ACCESS_TOKEN']
except KeyError as e:
    print("QUAY_ACCESS_TOKEN environment variable is not set. Please, set it before running the script.")
    exit('Script exiting....')

REGISTRY  = "quay.io"
NAMESPACE = "kiegroup"

IMAGES={"kogito-data-index-nightly","kogito-quarkus-ubi8-nightly",
        "kogito-quarkus-jvm-ubi8-nightly","kogito-quarkus-ubi8-s2i-nightly",
        "kogito-springboot-ubi8-nightly","kogito-springboot-ubi8-s2i-nightly",
        "kogito-jobs-service-nightly","kogito-management-console-nightly",
        "kogito-cloud-operator-nightly"
    }
def get_image_tags(image):
    '''
    Get all the available tags for the image
    :param image: image name whose tags needs to be fetched
    :return: tags: List of a strcut with tagName and lastModified as fields
    '''
    tags = []
    r = requests.get('https://{0}/api/v1/repository/{1}/{2}/tag/?onlyActiveTags=true'.format(REGISTRY,NAMESPACE,image) , headers={'content-type': 'application/json', 'Authorization': 'Bearer ' + QUAY_ACCESS_TOKEN })
    image_metadata= json.loads(r.text)
    num_tags = len(image_metadata['tags'])
    for i in range(num_tags):
        tags.append({
            "tagName" : image_metadata['tags'][i]['name'], 
            "lastModified" : parse(image_metadata['tags'][i]['last_modified'])
        })

    return tags

def delete_image_tags(image, tags):
    '''
    Deletes the extra image tags from the repository
    :param image: Image whose tags needs to be deleted
    :param tags: List of struct with `tagName` and `last_modified` as fields for the image that needs to be deleted
    '''
    if len(tags) == 0:
        print("Image {} does not have extra tags that needs to be deleted".format(image))
    else:
        for tag in tags:
            requests.delete('https://{0}/api/v1/repository/{1}/{2}/tag/{3}'.format(REGISTRY,NAMESPACE,image,tag['tagName']) , headers={'content-type': 'application/json', 'Authorization': 'Bearer ' + QUAY_ACCESS_TOKEN })
        print("Successfully deleted {} tags for the image {}".format(len(tags),image))


def get_and_delete_old_tags(image,max_tags):
    '''
    Driver function, calls the `get_image_tags` to get all the available tags for a image
    finds the tags that needs to be deleted and then passes them to `delete_image_tags`
    :param image: image name whose old tags needs to be deleted
    :param max_tags: Number of maximum tags to be kept for the image
    '''
    all_tags = get_image_tags(image)
    
    all_tags = list(filter(lambda tag: tag["tagName"]!="latest", all_tags))   #Filter out the entry with latest as tagName from the struct list
    all_tags.sort(key=lambda tagInfo: tagInfo.get("lastModified"))  #sorting in ascending order to get oldest tag on top
    delete_tags = []
    if (len(all_tags) - max_tags) > 0:
        delete_tags = all_tags[:len(all_tags) - max_tags]
    
    delete_image_tags(image,delete_tags)
    


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Removes extra tags from the registry')
    parser.add_argument('--max-tags', dest='max_tags', default=50,type=int, help='Defines the maximum number of tags for the image to be available, defaults to 10')
    args = parser.parse_args()

    for image in IMAGES:
        get_and_delete_old_tags(image,args.max_tags)
