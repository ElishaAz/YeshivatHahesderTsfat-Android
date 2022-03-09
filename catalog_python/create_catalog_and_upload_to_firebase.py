#
# Scans a Google Drive folder and all of its sub-folders,
# creates a catalog of the files and uploads it to firebase.
#
# Usage:
#  - Create a local_fields.py file with the following:
# ```
# GOOGLE_DRIVE_KEY = <YOUR_GOOGLE_DRIVE_API_KEY>
# FIREBASE_SDK_PATH = <PATH_TO_SDK_JSON_FILE>
# ROOT_FOLDER = <ROOT_FOLDER_ID_IN_GOOGLE_DRIVE>
# ```
# You might also want to edit the `FIREBASE_COLLECTION` and `FIREBASE_DOCUMENT` variables below

import json
from queue import Queue
from typing import List
from urllib.request import urlopen
from local_fields import GOOGLE_DRIVE_KEY, FIREBASE_SDK_PATH, ROOT_FOLDER

import firebase_admin
from firebase_admin import firestore
from google.cloud.firestore import Client

FIREBASE_COLLECTION = u'catalog_v1'
FIREBASE_DOCUMENT = u'audio_catalog'

DRIVE_QUERY = "https://www.googleapis.com/drive/v3/files?q={0}&fields=files(id,name,mimeType,createdTime," \
              "parents)&key={1}"


class DriveQuery:
    # noinspection PyPep8Naming,PyShadowingNames,PyShadowingBuiltins
    def __init__(self, files):
        self.name = None
        self.files = [DriveQueryItem(**x) for x in files]

    def __str__(self):
        return F"DriveQuery[{','.join((str(x) for x in self.files))}]"


class DriveQueryItem:
    # noinspection PyPep8Naming,PyShadowingNames,PyShadowingBuiltins
    def __init__(self, id: str, name: str, mimeType: str, createdTime: str, parents: List[str]):
        self.id = id
        self.name = name
        self.mimeType = mimeType
        self.createdTime = createdTime
        self.parents = parents

    def to_catalog(self, parent_name: str) -> "CatalogItem":
        return CatalogItem(self.id, self.name, parent_name, self.createdTime)

    def __str__(self) -> str:
        return F"DriveQueryItem[{self.__dict__}]"


class Catalog:
    # noinspection PyPep8Naming,PyShadowingNames,PyShadowingBuiltins
    def __init__(self, lessons: List["CatalogItem"]):
        self.music = lessons

    def to_dict(self):
        return {"lessons": [x.to_dict() for x in self.music]}


class CatalogItem:
    # noinspection PyPep8Naming,PyShadowingNames,PyShadowingBuiltins
    def __init__(self, id, title, folder, createdTime):
        self.id = id
        self.title = title
        self.folder = folder
        self.createdTime = createdTime

    def to_dict(self):
        return self.__dict__


def get_query(id: str) -> str:
    return DRIVE_QUERY.format(F"'{id}'+in+parents", GOOGLE_DRIVE_KEY)


def get_request(query: str) -> DriveQuery:
    url = urlopen(query)
    json_url = json.load(url)
    return DriveQuery(**json_url)


if __name__ == '__main__':
    files: List["CatalogItem"] = []
    folders: "Queue[DriveQuery]" = Queue()
    root = get_request(get_query(ROOT_FOLDER))
    root.name = "root"
    folders.put(root)

    while not folders.empty():
        folder = folders.get()
        print(F"Scanning {folder.name}")
        for file in folder.files:
            if file.mimeType == "application/vnd.google-apps.folder":
                # Folder
                new_folder = get_request(get_query(file.id))
                new_folder.name = file.name
                folders.put(new_folder)
            elif file.mimeType.startswith("audio/"):
                # audio file

                # Remove file type if exists
                if file.name[-4] == '.':  # if the fourth from the end is a period
                    # if the last three are ascii
                    if file.name[-3].isascii() and file.name[-2].isascii() and file.name[-1].isascii():
                        # remove the last four characters
                        file.name = file.name[:-4]

                files.append(file.to_catalog(folder.name))
            else:
                print(F"Unknown mime type: {file.mimeType}")

    files.sort(key=lambda x: x.createdTime, reverse=True)
    catalog = Catalog(files)

    cred = firebase_admin.credentials.Certificate(FIREBASE_SDK_PATH)
    client: Client = firestore.client(firebase_admin.initialize_app(cred))
    client.collection(FIREBASE_COLLECTION).document(FIREBASE_DOCUMENT).set(catalog.to_dict())
